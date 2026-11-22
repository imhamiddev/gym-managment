package dev.imhamid.gymyar.ui.athlete

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.imhamid.gymyar.data.Api
import dev.imhamid.gymyar.data.Repo
import dev.imhamid.gymyar.data.Res
import dev.imhamid.gymyar.ui.components.*
import dev.imhamid.gymyar.ui.theme.gym
import dev.imhamid.gymyar.util.Qr
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Decodes QR codes from the camera's luminance plane with ZXing (works without Google Play services). */
private class QrAnalyzer(private val onFound: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            ),
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            val source = PlanarYUVLuminanceSource(data, plane.rowStride, image.height, 0, 0, image.width, image.height, false)
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            onFound(result.text)
        } catch (_: Exception) {
            // nothing readable in this frame
        } finally {
            reader.reset()
            image.close()
        }
    }
}

@Composable
private fun CameraPreview(onText: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val latest by rememberUpdatedState(onText)

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor, QrAnalyzer { text -> main.execute { latest(text) } })
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, main)
        onDispose {
            runCatching { future.get().unbindAll() }
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
fun ScannerScreen(vm: AthleteViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val g = MaterialTheme.gym
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; asked = true }
    val busy = remember { AtomicBoolean(false) }
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var working by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    fun handle(text: String) {
        if (!busy.compareAndSet(false, true)) return
        val parsed = Qr.parse(text)
        if (parsed == null) {
            message = false to "این QR مربوط به باشگاه‌یار نیست."
            return
        }
        working = true
        scope.launch {
            when (val r = Repo.act("check_in", Api.params { put("p_gym", if (parsed.first.startsWith("cm")) vm.state.value.data.membership?.gym?.id ?: parsed.first else parsed.first); put("p_token", parsed.second) })) {
                is Res.Ok -> {
                    val gym = r.value["gym_name"]?.jsonPrimitive?.contentOrNull
                    message = true to (if (gym != null) "حضور شما در «$gym» ثبت شد." else "حضور شما ثبت شد.")
                    vm.refresh()
                }
                is Res.Fail -> message = false to r.message
            }
            working = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            CameraPreview(onText = { handle(it) }, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier.align(Alignment.Center).size(250.dp)
                    .border(4.dp, g.lime, RoundedCornerShape(32.dp)),
            )
            Text(
                "QR باشگاه را داخل کادر بگیرید",
                color = Color.White, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                Text(
                    "برای اسکن QR به دسترسی دوربین نیاز داریم.",
                    color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
                )
                PrimaryButton("اجازه‌ی دسترسی به دوربین", onClick = {
                    if (asked) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    } else launcher.launch(Manifest.permission.CAMERA)
                })
            }
        }

        FilledTonalIconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
        ) { Icon(Icons.Rounded.Close, contentDescription = "بستن") }

        if (working) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
    }

    message?.let { (ok, text) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (ok) "ثبت شد" else "ثبت نشد", color = if (ok) g.success else g.danger) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    if (ok) onClose() else busy.set(false)
                }) { Text(if (ok) "باشه" else "تلاش دوباره") }
            },
            dismissButton = if (!ok) ({ TextButton(onClick = onClose) { Text("بستن") } }) else null,
        )
    }
}
