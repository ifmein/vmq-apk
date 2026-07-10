package vmq.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vone.qrcode.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import vmq.ui.theme.VmqTheme
import java.util.concurrent.Executors

class CaptureActivity : ComponentActivity() {
    private val qrCodeReader = QRCodeReader()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VmqTheme {
                CaptureScreen(
                    onBarcodeScanned = { result ->
                        returnScanResult(result)
                    },
                    onAlbumScan = { bitmap ->
                        val result = decodeQRCodeFromBitmap(bitmap)
                        if (result != null) {
                            returnScanResult(result)
                        } else {
                            Toast.makeText(this@CaptureActivity, getString(R.string.scan_failed), Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }

    private fun returnScanResult(result: String) {
        val resultIntent = Intent().apply {
            putExtra(SCAN_RESULT, result)
            putExtra(SCAN_RESULT_FORMAT, "QR_CODE")
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun decodeQRCodeFromBitmap(bitmap: android.graphics.Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf<DecodeHintType, Any>(DecodeHintType.CHARACTER_SET to "UTF8")

        return try {
            qrCodeReader.decode(binaryBitmap, hints).text
        } catch (e: Exception) {
            null
        } finally {
            qrCodeReader.reset()
        }
    }

    companion object {
        private const val SCAN_RESULT = "SCAN_RESULT"
        private const val SCAN_RESULT_FORMAT = "SCAN_RESULT_FORMAT"
    }
}

@Composable
fun CaptureScreen(
    onBarcodeScanned: (String) -> Unit,
    onAlbumScan: (android.graphics.Bitmap) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Album picker launcher
    val albumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        onAlbumScan(bitmap)
                    } else {
                        Toast.makeText(context, context.getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.scan_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val permission = Manifest.permission.CAMERA
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    // Handle scan result
    LaunchedEffect(scannedResult) {
        scannedResult?.let {
            onBarcodeScanned(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onBarcodeDetected = { result ->
                    if (scannedResult == null) {
                        scannedResult = result
                    }
                }
            )

            // Scan frame overlay
            ScanFrameOverlay(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // No permission state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.capture_camera_permission_required),
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Album scan button
        Button(
            onClick = { albumLauncher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.capture_album_scan),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastScannedTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                val scanner = BarcodeScanning.getClient()
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            if (barcode.valueType == Barcode.TYPE_TEXT ||
                                                barcode.valueType == Barcode.TYPE_URL) {
                                                barcode.rawValue?.let { value ->
                                                    // Throttle: only scan once per 2 seconds
                                                    val currentTime = System.currentTimeMillis()
                                                    if (currentTime - lastScannedTime > 2000) {
                                                        lastScannedTime = currentTime
                                                        onBarcodeDetected(value)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Semi-transparent overlay with cutout effect
        val frameSize = 240.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top area - dark overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Middle area - frame with transparent center
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left - dark overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(frameSize)
                        .background(Color.Black.copy(alpha = 0.5f))
                )

                // Center - transparent (scan area)
                Box(
                    modifier = Modifier.size(frameSize)
                )

                // Right - dark overlay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .size(frameSize)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            // Bottom area - dark overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Instruction text
        Text(
            text = stringResource(R.string.capture_scan_hint),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
        )
    }
}
