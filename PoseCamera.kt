package app.upwake.mission

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors

/** One analysed camera frame: landmarks in image pixels, already rotated upright. */
class PoseFrame(
    val pose: Pose,
    /** Upright image size (after rotation). */
    val width: Int,
    val height: Int,
    val mirrored: Boolean,
)

/**
 * Camera preview + on-device ML Kit pose detection. Uses the front camera when there is one,
 * so you can see yourself. Nothing is recorded or uploaded.
 */
@Composable
fun PoseCamera(
    previewView: PreviewView,
    modifier: Modifier = Modifier,
    onFrame: (PoseFrame) -> Unit,
    onError: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val frameCb by rememberUpdatedState(onFrame)
    val errorCb by rememberUpdatedState(onError)

    DisposableEffect(owner, previewView) {
        val future = ProcessCameraProvider.getInstance(ctx)
        val executor = Executors.newSingleThreadExecutor()
        val detector = PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build(),
        )
        val main = Handler(Looper.getMainLooper())
        var provider: ProcessCameraProvider? = null
        var disposed = false

        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get()
                provider = p
                val front = p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = buildPoseAnalysis(detector, executor, front) { f ->
                    main.post { if (!disposed) frameCb(f) }
                }
                p.unbindAll()
                p.bindToLifecycle(owner, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e("UpwakePose", "Camera failed", e)
                errorCb(e.message ?: "Camera error")
            }
        }, ContextCompat.getMainExecutor(ctx))

        onDispose {
            disposed = true
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
            }
            detector.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun buildPoseAnalysis(
    detector: com.google.mlkit.vision.pose.PoseDetector,
    executor: java.util.concurrent.Executor,
    mirrored: Boolean,
    onFrame: (PoseFrame) -> Unit,
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
        val rot = proxy.imageInfo.rotationDegrees
        val upright = rot == 90 || rot == 270
        val w = if (upright) proxy.height else proxy.width
        val h = if (upright) proxy.width else proxy.height
        detector.process(InputImage.fromMediaImage(media, rot))
            .addOnSuccessListener { pose -> onFrame(PoseFrame(pose, w, h, mirrored)) }
            .addOnCompleteListener { proxy.close() }
    }
    return analysis
}
