package app.upwake.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.upwake.ui.theme.W
import app.upwake.ui.theme.WIcons

object Prefs {
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("upwake_prefs", Context.MODE_PRIVATE)
    fun onboarded(ctx: Context) = p(ctx).getBoolean("onboarded", false)
    fun setOnboarded(ctx: Context) = p(ctx).edit().putBoolean("onboarded", true).apply()
}

/** The Upwake mark: an alarm-clock sun rising over the horizon. */
@Composable
fun SunriseMark(modifier: Modifier = Modifier) = UpwakeLogo(modifier)

@Composable
fun UpwakeLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(app.upwake.R.drawable.logo_mark),
        contentDescription = "Upwake",
        modifier = modifier,
    )
}

/**
 * Google Play requires a prominent disclosure before sending people to Accessibility settings.
 */
@Composable
fun StrictModeDisclosure(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = W.Card,
        titleContentColor = W.Text,
        textContentColor = W.Text2,
        title = { Text("Turn on strict mode?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Upwake uses Android's Accessibility service only while an alarm is ringing or your morning " +
                    "check-in is unfinished. It closes the power menu and notification shade, and brings the alarm back " +
                    "if you open another app.\n\nUpwake does not read, store or share anything on your screen, and " +
                    "it does nothing at any other time. You can turn it off whenever you like in Settings.",
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text("Agree & open settings", color = W.Dawn, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now", color = W.Text2) }
        },
    )
}

@Composable
fun OnboardingScreen(onFinish: (createAlarm: Boolean) -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = page > 0) { page-- }

    Column(Modifier.fillMaxSize().background(W.Bg).systemBarsPadding()) {
        // progress
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..3) {
                Box(
                    Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= page) W.Dawn else W.Card2),
                )
            }
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "onboarding",
            modifier = Modifier.weight(1f),
        ) { p ->
            when (p) {
                0 -> WelcomePage { page = 1 }
                1 -> FeaturesPage { page = 2 }
                2 -> PermissionsPage { page = 3 }
                else -> ReadyPage(onCreate = { onFinish(true) }, onExplore = { onFinish(false) })
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        SunriseMark(Modifier.size(180.dp))
        Text(
            "Upwake", color = W.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "The alarm clock you can't\nsnooze your way out of.",
            color = W.Text2, fontSize = 18.sp, lineHeight = 25.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.weight(1.3f))
        PrimaryButton("Get started", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FeaturesPage(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("How Upwake works", color = W.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Spacer(Modifier.height(28.dp))
        Feature(WIcons.Camera, "Missions get you out of bed", "Snap a photo of your sink or scan your toothpaste. You can only do that standing up.")
        Feature(WIcons.Lock, "No way around it", "Until the mission is done, the alarm stays on top: no Home button, no power menu.")
        Feature(WIcons.Moon, "Sleep focus", "Social apps lock 30 minutes before bed and stay locked until 30 minutes after you wake.")
        Feature(WIcons.Sun, "Start with gratitude", "A short reflection each morning before your day begins.")
        Spacer(Modifier.weight(1f))
        PrimaryButton("Continue", onClick = onNext, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(W.DawnDim),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = W.Dawn, modifier = Modifier.size(22.dp)) }
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, color = W.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = W.Text2, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun PermissionsPage(onNext: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val checks = remember(tick) { Health.checks(ctx) }
    val missingRequired = checks.filter { it.required && it.ok == false }
    var disclosure by remember { mutableStateOf<Health.Check?>(null) }
    var pending by remember { mutableStateOf<Health.Fix.Permission?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val fb = pending
        if (!granted && fb != null) Health.open(ctx, fb.fallback)
        pending = null
        tick++
    }

    fun fix(c: Health.Check) {
        if (c.key == "guard") {
            disclosure = c
            return
        }
        when (val f = c.fix) {
            is Health.Fix.Permission -> {
                pending = f
                permLauncher.launch(f.permission)
            }
            is Health.Fix.Open -> Health.open(ctx, f.intents)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("Make sure it always rings", color = W.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, lineHeight = 36.sp)
        Text(
            "Android blocks alarm apps from taking over the screen until you allow it. Tap each one.",
            color = W.Text2, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionCard {
                checks.forEachIndexed { i, c ->
                    if (i > 0) RowDivider()
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = c.ok != true) { fix(c) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(if (c.ok == true) W.Dawn else W.Card2),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c.ok == true) Icon(WIcons.Check, null, tint = W.OnDawn, modifier = Modifier.size(14.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
                            Text(
                                if (c.key == "guard") "Strict mode" else c.title,
                                color = if (c.ok == true) W.Text2 else W.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            )
                            if (!c.required) Text("Optional", color = W.Text3, fontSize = 12.sp)
                        }
                        if (c.ok != true) Pill(if (c.ok == null) "Open" else "Allow", { fix(c) }, filled = c.required)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            if (missingRequired.isEmpty()) "Continue" else "${missingRequired.size} left",
            onClick = { if (missingRequired.isEmpty()) onNext() else fix(missingRequired.first()) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (missingRequired.isNotEmpty()) {
            Text(
                "Skip for now",
                color = W.Text3, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp).clickable(onClick = onNext),
            )
        }
    }

    disclosure?.let { c ->
        StrictModeDisclosure(
            onAgree = {
                disclosure = null
                val f = c.fix
                if (f is Health.Fix.Open) Health.open(ctx, f.intents)
            },
            onDismiss = { disclosure = null },
        )
    }
}

@Composable
private fun ReadyPage(onCreate: () -> Unit, onExplore: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(W.DawnDim),
            contentAlignment = Alignment.Center,
        ) { Icon(WIcons.Check, null, tint = W.Dawn, modifier = Modifier.size(56.dp)) }
        Text(
            "You're all set.", color = W.Text, fontSize = 32.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            "Set your first alarm. Tomorrow morning,\nyou're getting up.",
            color = W.Text2, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.weight(1.3f))
        PrimaryButton("Create my first alarm", onClick = onCreate, modifier = Modifier.fillMaxWidth())
        Text(
            "Look around first", color = W.Text2, fontSize = 15.sp,
            modifier = Modifier.padding(top = 14.dp).clickable(onClick = onExplore).padding(6.dp),
        )
    }
}
