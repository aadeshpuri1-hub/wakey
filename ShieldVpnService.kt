package app.upwake.focus

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import app.upwake.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * "Upwake Shield": a local, DNS-only VPN. Only DNS lookups enter it; all other traffic goes
 * straight to the internet as usual, and nothing leaves the phone except normal DNS queries.
 *
 *  - Adult sites (always, when on): lookups go to Cloudflare for Families (1.1.1.3), which refuses adult domains.
 *  - Social sites (only during the sleep window): domains of the blocked apps get "does not exist".
 */
class ShieldVpnService : VpnService() {

    companion object {
        private const val TAG = "UpwakeShield"
        private const val TUN_IP = "10.111.222.1"
        private const val DNS_IP = "10.111.222.2"

        /** Firefox/Chrome use these to decide whether to bypass the system DNS. Always blocked. */
        private val BYPASS = setOf(
            "use-application-dns.net", "dns.google", "dns.google.com", "cloudflare-dns.com",
            "mozilla.cloudflare-dns.com", "chrome.cloudflare-dns.com", "doh.opendns.com",
            "dns.quad9.net", "dns.adguard.com", "dns.nextdns.io", "doh.cleanbrowsing.org",
        )

        @Volatile
        var running = false
            private set

        private var instance: ShieldVpnService? = null

        fun start(ctx: Context) {
            try {
                ctx.startService(Intent(ctx, ShieldVpnService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "start failed", e)
            }
        }

        fun stop() {
            instance?.shutdown()
            instance?.stopSelf()
        }
    }

    private var tun: ParcelFileDescriptor? = null
    private var reader: Thread? = null
    private var pool: ExecutorService? = null
    private var out: FileOutputStream? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tun == null) establish()
        return START_STICKY
    }

    override fun onRevoke() {
        shutdown()
        super.onRevoke()
    }

    override fun onDestroy() {
        shutdown()
        instance = null
        super.onDestroy()
    }

    private fun establish() {
        val b = Builder()
            .setSession("Upwake Shield")
            .addAddress(TUN_IP, 32)
            .addDnsServer(DNS_IP)
            .addRoute(DNS_IP, 32)
            .setMtu(1500)
            .setConfigureIntent(
                PendingIntent.getActivity(
                    this, 7, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        if (Build.VERSION.SDK_INT >= 29) b.setBlocking(true)
        val fd = try {
            b.establish()
        } catch (e: Exception) {
            Log.e(TAG, "establish failed", e)
            null
        } ?: run {
            stopSelf()
            return
        }
        tun = fd
        out = FileOutputStream(fd.fileDescriptor)
        pool = Executors.newFixedThreadPool(4)
        running = true
        reader = Thread({ readLoop(fd) }, "upwake-dns").also { it.start() }
    }

    fun shutdown() {
        running = false
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
        reader?.interrupt()
        reader = null
        pool?.shutdownNow()
        pool = null
        out = null
    }

    // ------------------------------------------------------------------ packets

    private fun readLoop(fd: ParcelFileDescriptor) {
        val input = FileInputStream(fd.fileDescriptor)
        val buf = ByteArray(32767)
        while (running) {
            val n = try {
                input.read(buf)
            } catch (e: Exception) {
                break
            }
            if (n <= 0) {
                try {
                    Thread.sleep(20)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }
            try {
                handle(buf.copyOf(n))
            } catch (e: Exception) {
                Log.w(TAG, "bad packet", e)
            }
        }
    }

    private fun handle(pkt: ByteArray) {
        if (pkt.size < 28 || (pkt[0].toInt() shr 4) != 4) return // IPv4 only
        val ihl = (pkt[0].toInt() and 0x0F) * 4
        if ((pkt[9].toInt() and 0xFF) != 17) return // UDP only
        if (pkt.size < ihl + 8 + 12) return
        val dport = ((pkt[ihl + 2].toInt() and 0xFF) shl 8) or (pkt[ihl + 3].toInt() and 0xFF)
        if (dport != 53) return

        val dnsOff = ihl + 8
        val dns = pkt.copyOfRange(dnsOff, pkt.size)
        val name = qname(dns)
        val cfg = FocusStore.get(this)

        if (name != null && shouldBlock(name, cfg)) {
            nxdomain(dns)?.let { write(wrap(pkt, ihl, it, it.size)) }
            return
        }
        val upstreams = if (cfg.adultBlock) listOf("1.1.1.3", "1.0.0.3") else listOf("1.1.1.1", "1.0.0.1")
        pool?.execute { forward(pkt, ihl, dns, upstreams) }
    }

    private fun shouldBlock(name: String, cfg: FocusConfig): Boolean {
        if (matches(name, BYPASS)) return true
        if (cfg.isActive() && matches(name, SocialCatalog.domainsFor(cfg.apps))) return true
        return false
    }

    private fun matches(name: String, domains: Set<String>): Boolean =
        domains.any { name == it || name.endsWith(".$it") }

    private fun forward(pkt: ByteArray, ihl: Int, dns: ByteArray, upstreams: List<String>) {
        for (ip in upstreams) {
            try {
                DatagramSocket().use { s ->
                    protect(s)
                    s.soTimeout = 4000
                    s.send(DatagramPacket(dns, dns.size, InetAddress.getByName(ip), 53))
                    val rb = ByteArray(4096)
                    val rp = DatagramPacket(rb, rb.size)
                    s.receive(rp)
                    write(wrap(pkt, ihl, rb, rp.length))
                }
                return
            } catch (e: Exception) {
                // try the next upstream
            }
        }
    }

    private fun write(packet: ByteArray) {
        val o = out ?: return
        synchronized(o) {
            try {
                o.write(packet)
            } catch (_: Exception) {
            }
        }
    }

    /** Builds the IPv4+UDP reply to [req] carrying [payload]. */
    private fun wrap(req: ByteArray, ihl: Int, payload: ByteArray, len: Int): ByteArray {
        val total = 20 + 8 + len
        val p = ByteArray(total)
        p[0] = 0x45
        p[2] = (total shr 8).toByte()
        p[3] = total.toByte()
        p[6] = 0x40 // don't fragment
        p[8] = 64 // TTL
        p[9] = 17 // UDP
        System.arraycopy(req, 16, p, 12, 4) // src = original dst
        System.arraycopy(req, 12, p, 16, 4) // dst = original src
        var sum = 0
        for (i in 0 until 20 step 2) sum += ((p[i].toInt() and 0xFF) shl 8) or (p[i + 1].toInt() and 0xFF)
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        val ck = sum.inv() and 0xFFFF
        p[10] = (ck shr 8).toByte()
        p[11] = ck.toByte()
        // UDP header: swap ports, checksum 0 (optional for IPv4)
        p[20] = req[ihl + 2]; p[21] = req[ihl + 3]
        p[22] = req[ihl]; p[23] = req[ihl + 1]
        val ulen = 8 + len
        p[24] = (ulen shr 8).toByte()
        p[25] = ulen.toByte()
        System.arraycopy(payload, 0, p, 28, len)
        return p
    }

    /** Question name, lower-case, or null if it can't be parsed. */
    private fun qname(dns: ByteArray): String? {
        if (dns.size < 13) return null
        var i = 12
        val sb = StringBuilder()
        while (i < dns.size) {
            val l = dns[i].toInt() and 0xFF
            if (l == 0) return sb.toString().lowercase()
            if (l and 0xC0 != 0) return null
            i++
            if (i + l > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (k in 0 until l) sb.append((dns[i + k].toInt() and 0xFF).toChar())
            i += l
        }
        return null
    }

    /** "This domain doesn't exist" reply to the query. */
    private fun nxdomain(q: ByteArray): ByteArray? {
        var i = 12
        while (i < q.size && (q[i].toInt() and 0xFF) != 0) i += (q[i].toInt() and 0xFF) + 1
        val end = i + 1 + 4 // zero byte + QTYPE + QCLASS
        if (end > q.size) return null
        val r = q.copyOf(end)
        r[2] = ((q[2].toInt() or 0x80) and 0xFB).toByte() // QR=1, AA=0, keep opcode + RD
        r[3] = 0x83.toByte() // RA=1, RCODE=3 NXDOMAIN
        r[4] = 0; r[5] = 1 // 1 question
        for (k in 6..11) r[k] = 0 // no answers / authority / additional
        return r
    }
}
