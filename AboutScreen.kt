package app.upwake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.BuildConfig
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons

/** Plain-language privacy policy. Mirrors PRIVACY.md (the public Google Play privacy-policy page). */
val PRIVACY_POLICY = listOf(
    "Who we are" to
        "Upwake is made by Aditya Puri, an independent developer in India. Contact: $CONTACT_EMAIL.",
    "The short version" to
        "Upwake has no account, no sign-in, no ads, no analytics and no server of its own. Your alarms, mission photos and barcodes, gratitude journal and focus settings are stored only on this phone. We never see them, sell them or share them.",
    "Camera" to
        "Used only for photo, barcode, squat and push-up missions. Your reference photo is saved privately inside the app. Squats and push-ups are checked live on the device by Google ML Kit pose detection: the video is never recorded, stored or uploaded.",
    "Barcode reading and pose detection (Google ML Kit)" to
        "Barcodes and exercise form are analysed on the device by Google ML Kit. Images never leave your phone, but Google ML Kit may send Google basic, non-personal diagnostic and performance information (such as the device model and how long a scan took) to improve the service, under Google's privacy policy.",
    "Usage access" to
        "Used only during your sleep-focus window to notice when an app you chose to block is opened. It is checked in real time and not recorded.",
    "Upwake Shield (VPN)" to
        "A filter that runs on your phone. It only looks at the website names your phone looks up (DNS), to block adult sites and, during sleep focus, the websites of apps you blocked. Lookups are forwarded to Cloudflare public DNS (1.1.1.3 or 1.1.1.1), which handles them under Cloudflare's privacy policy. Upwake does not see the pages you visit and keeps no log.",
    "Accessibility (strict mode, optional)" to
        "Only active while an alarm is ringing or your morning check-in is unfinished. It only checks which app or window has come to the front (for example, to recognise the power menu) so it can close the power menu and notification shade and bring the alarm back. It cannot read the contents of your screen, never sees what you type, and stores or shares nothing.",
    "Other permissions" to
        "Notifications, exact alarms, full-screen alarms, display over other apps, battery optimisation and start-on-boot are used only to ring on time, show the alarm above everything else and resume an alarm after a restart. They collect no data.",
    "Backups" to
        "Upwake opts out of Android cloud backup and device-to-device transfer, so your journal and photos are never copied to a cloud account.",
    "Keeping your data safe" to
        "Your data lives in Upwake's private storage, which other apps cannot read. The app makes no unencrypted network connections.",
    "Deleting your data" to
        "Delete individual items in the app, or delete everything at once by uninstalling Upwake or using Android Settings → Apps → Upwake → Storage → Clear data. There is nothing stored with us to delete.",
    "Your rights" to
        "Because we hold no personal data, there is nothing for us to access, correct or erase on our side. You still have all rights given by the law where you live, including under India's Digital Personal Data Protection Act, 2023, and you can contact us at any time.",
    "Children" to
        "Upwake is not directed at children under 13 and we do not knowingly collect data from anyone.",
    "Changes" to
        "If this policy changes in a meaningful way, the app will show you the new version and ask you to accept it.",
)

@Composable
fun AboutScreen(onBack: () -> Unit, onLegal: (tab: Int) -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(WIcons.Close, "Close", tint = W.Text, modifier = Modifier.size(22.dp)) }
            Text("About", color = W.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                SunriseMark(Modifier.size(110.dp))
                Text("Upwake", color = W.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Version ${BuildConfig.VERSION_NAME}", color = W.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(28.dp))
            SectionTitle("Legal")
            SectionCard {
                SettingRow(WIcons.Book, "Terms of Use", onClick = { onLegal(0) })
                RowDivider()
                SettingRow(WIcons.Shield, "Privacy Policy", onClick = { onLegal(1) })
            }
            Text(
                "Questions or complaints: $CONTACT_EMAIL",
                color = W.Text3, fontSize = 12.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(start = 6.dp, top = 10.dp),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
