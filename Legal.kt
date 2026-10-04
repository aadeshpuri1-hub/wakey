package app.upwake.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons

/**
 * Bump this whenever TERMS or PRIVACY_POLICY change in a meaningful way.
 * Everyone who accepted an older version is asked to accept again.
 */
const val LEGAL_VERSION = 1
const val LEGAL_DATE = "4 October 2026"
const val CONTACT_EMAIL = "adityapuri2001@gmail.com"

object LegalPrefs {
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("upwake_prefs", Context.MODE_PRIVATE)
    fun accepted(ctx: Context) = p(ctx).getInt("legal_version", 0) >= LEGAL_VERSION
    fun accept(ctx: Context) = p(ctx).edit()
        .putInt("legal_version", LEGAL_VERSION)
        .putLong("legal_accepted_at", System.currentTimeMillis())
        .apply()
}

/** Terms of Use. Mirrors TERMS.md in the repository. */
val TERMS = listOf(
    "1. Agreement" to
        "These Terms of Use are an agreement between you and Aditya Puri, the developer of Upwake (\"we\", \"us\"). By installing or using Upwake you agree to these Terms and to the Privacy Policy. If you do not agree, do not use the app and uninstall it.",
    "2. Who can use Upwake" to
        "You must be at least 13 years old. If you are under 18 (or the age of majority where you live), you may only use Upwake with the permission and supervision of a parent or guardian, who accepts these Terms on your behalf.",
    "3. What Upwake is" to
        "Upwake is a personal alarm clock with optional wake-up missions, a morning gratitude check-in, sleep focus (blocking apps and websites you choose during your sleep window) and a website filter. It is a self-discipline tool you configure for yourself. It is not a medical device and does not diagnose, treat or prevent any condition.",
    "4. Alarms are not guaranteed" to
        "We work hard to make alarms reliable, but Android, your phone's manufacturer, battery savers, system updates, a dead or switched-off phone, muted volume, missing permissions or a bug can stop an alarm from ringing on time or at all. Do not rely on Upwake as your only alarm for anything important — such as work, travel, exams, taking medication or caring for others. Always keep a backup.",
    "5. Strict mode and lockdown — your choice, your risk" to
        "When you turn on strict mode, Upwake deliberately makes it hard to stop a ringing alarm: it keeps the alarm on screen, closes the power menu and notification shade, raises the volume and comes back after a restart until you finish your mission and morning check-in. You turn this on yourself and can turn it off at any time when no alarm is ringing (Settings or Android Accessibility settings, or by uninstalling the app). Emergency calls stay available. Do not enable strict mode if you may need instant, unrestricted access to your phone when you wake up — for example for medical, caregiving or on-call reasons — or if loud sounds or being unable to silence an alarm could harm you or others. Set a volume that is safe for your hearing.",
    "6. Sleep focus and website filter" to
        "Sleep focus and Upwake Shield are best-effort tools. They can be bypassed (for example by other VPNs, private DNS, browsers with their own DNS, or system changes) and can miss sites or block sites they should not. They are not parental-control or security software and must not be relied on to protect children or anyone else. Strict sleep focus cannot be unlocked from inside the app until the window ends; you accept that you will not be able to use the apps you selected during that time.",
    "7. Your responsibilities" to
        "Use Upwake lawfully and safely. Do not use it while driving or in situations where an alarm or blocked phone could put you or others at risk. Only photograph places and barcodes you are allowed to, and do not include other people in mission photos without their consent. You are responsible for the content you write in the app, including your gratitude journal.",
    "8. Your data" to
        "Upwake stores your content only on your device. See the Privacy Policy for details. Because we do not keep a copy, we cannot recover anything you lose when you uninstall the app, clear its data or change phones.",
    "9. Licence" to
        "We grant you a personal, non-exclusive, non-transferable, revocable licence to use Upwake on devices you own or control, for personal, non-commercial purposes. You may not copy, modify, reverse engineer, decompile, resell or redistribute the app, except where the law expressly allows it. Upwake, its name, logo and design belong to us. Open-source components are covered by their own licences.",
    "10. Third-party services" to
        "Upwake relies on Android, Google Play, Google ML Kit (on-device barcode reading) and Cloudflare public DNS. These are run by third parties under their own terms and privacy policies; we are not responsible for them.",
    "11. No warranty" to
        "To the maximum extent allowed by law, Upwake is provided \"as is\" and \"as available\", without warranties of any kind, express or implied, including fitness for a particular purpose, accuracy, reliability or that it will be uninterrupted or error-free.",
    "12. Limitation of liability" to
        "To the maximum extent allowed by law, we are not liable for any indirect, incidental, special or consequential loss, or for any loss of income, employment, opportunity, data or goodwill, or for missed or late alarms, oversleeping, blocked or unblocked content, or anything that happens while strict mode or sleep focus is active. Our total liability for all claims relating to Upwake is limited to the amount you paid us for the app in the 12 months before the claim (which is zero for the free version). Nothing in these Terms limits liability that cannot be limited by law, or your rights as a consumer under applicable law.",
    "13. Indemnity" to
        "You agree to compensate us for losses and reasonable costs arising from your misuse of Upwake or your breach of these Terms, to the extent permitted by law.",
    "14. Changes and ending" to
        "We may update Upwake and these Terms. If we make important changes we will ask you to accept them again in the app. You can stop using Upwake at any time by uninstalling it. We may stop offering the app at any time.",
    "15. Governing law" to
        "These Terms are governed by the laws of India. Courts in India have jurisdiction over any dispute, without affecting any mandatory consumer protections of the country where you live.",
    "16. Contact" to
        "Questions, complaints or grievances: $CONTACT_EMAIL. We aim to acknowledge complaints within 7 days and resolve them within 30 days.",
)

/**
 * First launch: Alarmy-style consent ("Agree to all" + required items, each opening its document).
 * Otherwise: the Terms / Privacy reader.
 */
@Composable
fun LegalScreen(onAccept: (() -> Unit)? = null, onBack: (() -> Unit)? = null, startTab: Int = 0) {
    var reading by rememberSaveable { mutableStateOf<Int?>(if (onAccept == null) startTab else null) }
    val r = reading
    if (onAccept == null) {
        LegalReader(tab = r ?: startTab, onTab = { reading = it }, onBack = { onBack?.invoke() })
        return
    }
    if (r != null) {
        LegalReader(tab = r, onTab = { reading = it }, onBack = { reading = null })
        return
    }
    ConsentScreen(onAccept = onAccept, onRead = { reading = it })
}

@Composable
private fun ConsentScreen(onAccept: () -> Unit, onRead: (Int) -> Unit) {
    var age by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var risk by rememberSaveable { mutableStateOf(false) }
    val all = age && terms && privacy && risk

    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            UpwakeLogo(Modifier.size(96.dp))
            Text(
                "Welcome to Upwake", color = W.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "Please review and agree to continue.",
                color = W.Text2, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(W.Card)
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            ConsentRow("Agree to all", all, bold = true) { v ->
                age = v; terms = v; privacy = v; risk = v
            }
            HorizontalDivider(color = W.Line, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            ConsentRow("[Required] I am 13 or older, or have a parent's permission", age) { age = it }
            ConsentRow("[Required] Terms of Use", terms, onOpen = { onRead(0) }) { terms = it }
            ConsentRow("[Required] Privacy Policy", privacy, onOpen = { onRead(1) }) { privacy = it }
            ConsentRow(
                "[Required] I understand alarms can fail, and that Strict mode and Sleep focus are my own choice",
                risk,
            ) { risk = it }
            PrimaryButton(
                "Agree and start", onClick = onAccept, enabled = all,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ConsentRow(
    text: String,
    checked: Boolean,
    bold: Boolean = false,
    onOpen: (() -> Unit)? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (checked) W.Dawn else W.Card2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WIcons.Check, null, tint = if (checked) W.OnDawn else W.Text3, modifier = Modifier.size(14.dp))
        }
        Text(
            text, color = W.Text, fontSize = if (bold) 17.sp else 14.sp, lineHeight = 19.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp),
        )
        if (onOpen != null) {
            Text(
                "View", color = W.Text2, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpen).padding(6.dp),
            )
        }
    }
}

@Composable
private fun LegalReader(tab: Int, onTab: (Int) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ Back", color = W.Dawn, fontSize = 17.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack).padding(8.dp),
            )
        }
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(9.dp))
                .background(W.Card2).padding(2.dp),
        ) {
            listOf("Terms of Use", "Privacy Policy").forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(7.dp))
                        .background(if (tab == i) Color(0xFF636366) else W.Card2)
                        .clickable { onTab(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = W.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(
                if (tab == 0) "Terms of Use" else "Privacy Policy",
                color = W.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text("Last updated $LEGAL_DATE", color = W.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            (if (tab == 0) TERMS else PRIVACY_POLICY).forEach { (title, body) ->
                Text(
                    title, color = W.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(body, color = W.Text2, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
