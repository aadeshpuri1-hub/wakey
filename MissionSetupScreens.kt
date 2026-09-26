package com.aditya.wakey.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.mission.CameraGate
import com.aditya.wakey.mission.CameraPreview
import com.aditya.wakey.mission.PhotoMatcher
import com.aditya.wakey.mission.ScanFrame
import com.aditya.wakey.mission.ShutterButton
import com.aditya.wakey.mission.rememberPreviewView
import com.aditya.wakey.ui.theme.W

@Composable
private fun SetupTopBar(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color(0x99000000)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, textAlign = TextAlign.Center, fontSize = 15.sp,
        modifier = modifier.padding(16.dp)
            .background(Color(0xAA000000), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Take the reference photo. Returns the saved file path, or null if cancelled. */
@Composable
fun PhotoSetupScreen(alarmId: Int, onDone: (String?) -> Unit) {
    val ctx = LocalContext.current
    BackHandler { onDone(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraGate {
            val pv = rememberPreviewView()
            var shot by remember { mutableStateOf<Bitmap?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            val s = shot

            Box(Modifier.fillMaxSize()) {
                if (s == null) {
                    CameraPreview(pv, Modifier.fillMaxSize(), onError = { error = it })
                    Column(
                        Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Hint(error ?: "Pick a spot away from your bed, like the bathroom sink or kitchen. You'll need to photograph it again to turn the alarm off.")
                        ShutterButton { shot = pv.bitmap }
                    }
                } else {
                    Image(s.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Row(
                        Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { shot = null },
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text("Retake", color = Color.White) }
                        Button(
                            onClick = { onDone(PhotoMatcher.saveReference(ctx, alarmId, s)) },
                            colors = ButtonDefaults.buttonColors(containerColor = W.Accent),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) { Text("Use this photo", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        Box(Modifier.systemBarsPadding()) { SetupTopBar("Register photo") { onDone(null) } }
    }
}

/** Scan the barcode to register. Returns the raw value, or null if cancelled. */
@Composable
fun BarcodeSetupScreen(onDone: (String?) -> Unit) {
    BackHandler { onDone(null) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraGate {
            val pv = rememberPreviewView()
            var found by remember { mutableStateOf<String?>(null) }
            var error by remember { mutableStateOf<String?>(null) }

            Box(Modifier.fillMaxSize()) {
                CameraPreview(
                    pv, Modifier.fillMaxSize(),
                    scanBarcodes = true,
                    onBarcodes = { if (found == null) found = it.first() },
                    onError = { error = it },
                )
                ScanFrame(Modifier.align(Alignment.Center))
                val f = found
                Column(
                    Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (f == null) {
                        Hint(error ?: "Scan a barcode on something outside your bedroom: toothpaste, a shampoo bottle, a cereal box...")
                    } else {
                        Column(
                            Modifier.fillMaxWidth().background(W.Card, RoundedCornerShape(20.dp)).padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("✅ Barcode found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(f, color = W.Text2, modifier = Modifier.padding(vertical = 8.dp), maxLines = 2)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { found = null },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                ) { Text("Rescan", color = Color.White) }
                                Button(
                                    onClick = { onDone(f) },
                                    colors = ButtonDefaults.buttonColors(containerColor = W.Accent),
                                    modifier = Modifier.weight(1f).height(52.dp),
                                ) { Text("Use this", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.systemBarsPadding()) { SetupTopBar("Register barcode") { onDone(null) } }
    }
}
