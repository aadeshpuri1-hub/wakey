package com.aditya.wakey.mission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aditya.wakey.ui.theme.W
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun rememberPreviewView(): PreviewView {
    val ctx = LocalContext.current
    return remember {
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
}

/** Back-camera preview. If [onBarcodes] is given, also scans barcodes continuously. */
@Composable
fun CameraPreview(
    previewView: PreviewView,
    modifier: Modifier = Modifier,
    scanBarcodes: Boolean = false,
    onBarcodes: (List<String>) -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val barcodeCb by rememberUpdatedState(onBarcodes)
    val errorCb by rememberUpdatedState(onError)

    DisposableEffect(owner, previewView, scanBarcodes) {
        val future = ProcessCameraProvider.getInstance(ctx)
        val executor = Executors.newSingleThreadExecutor()
        val scanner: BarcodeScanner? = if (scanBarcodes) BarcodeScanning.getClient() else null
        val main = Handler(Looper.getMainLooper())
        var provider: ProcessCameraProvider? = null
        var disposed = false

        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val useCases = mutableListOf<UseCase>(preview)
                if (scanner != null) {
                    useCases += buildBarcodeAnalysis(scanner, executor) { values ->
                        main.post { if (!disposed) barcodeCb(values) }
                    }
                }
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases.toTypedArray())
            } catch (e: Exception) {
                Log.e("WakeyCam", "Camera failed", e)
                errorCb(e.message ?: "Camera error")
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            disposed = true
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
            }
            scanner?.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun buildBarcodeAnalysis(
    scanner: BarcodeScanner,
    executor: java.util.concurrent.Executor,
    onFound: (List<String>) -> Unit,
): ImageAnalysis {
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    analysis.setAnalyzer(executor) { proxy ->
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return@setAnalyzer
        }
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                val values = codes.mapNotNull { it.rawValue }.filter { it.isNotBlank() }
                if (values.isNotEmpty()) onFound(values)
            }
            .addOnCompleteListener { proxy.close() }
    }
    return analysis
}

fun hasCamera(ctx: android.content.Context) =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Shows [content] only once camera permission is granted; otherwise asks for it. */
@Composable
fun CameraGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasCamera(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().background(W.Bg).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(96.dp).background(W.AccentDim, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    com.aditya.wakey.ui.theme.WIcons.Camera, null,
                    tint = W.Accent, modifier = Modifier.size(44.dp),
                )
            }
            Text(
                "Camera access needed",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Photo and barcode missions use the camera to check you're really out of bed.",
                color = W.Text2, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = W.Accent),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Allow camera", fontWeight = FontWeight.Bold) }
        }
    }
}

/** Big round shutter button. */
@Composable
fun ShutterButton(enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(80.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .background(if (enabled) Color.White else Color.Gray, CircleShape),
    )
}

/** Scan-area frame drawn over the preview. */
@Composable
fun ScanFrame(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 280.dp, height = 180.dp)
            .border(3.dp, W.Accent, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
    )
}
