package rs.djerman.losmobileview

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.os.Build
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


@androidx.camera.core.ExperimentalGetImage
class BarcodeScannerActivity : AppCompatActivity() {

    // Prevents concurrent barcode processing
    private var processingBarcode = false

    // Viewfinder for camera preview
    private lateinit var previewView: PreviewView

    // Rectangular area where barcode must appear for acceptance
    private lateinit var overlayRect: Rect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        enableEdgeToEdge()
        val root = findViewById<View>(R.id.previewContainer)  // корен из XML-а
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        previewView = findViewById(R.id.previewView)

        // Draw a transparent overlay with a white scan box in the center
        val overlay = object : View(this) {
            private val paint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val viewWidth = width
                val viewHeight = height

                // Calculate scan box dimensions (centered, 60% width, 30% height)
                val left = (viewWidth * 0.2f).toInt()
                val top = (viewHeight * 0.35f).toInt()
                val right = (viewWidth * 0.8f).toInt()
                val bottom = (viewHeight * 0.65f).toInt()

                overlayRect = Rect(left, top, right, bottom)
                canvas.drawRect(overlayRect, paint)
            }
        }

        // Add the overlay view above the preview
        findViewById<FrameLayout>(R.id.previewContainer).addView(overlay)

        // Set a timeout to close the activity if no scan happens (30 seconds)
        previewView.postDelayed({
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, getString(R.string.scan_timeout), Toast.LENGTH_SHORT).show()
                finish()
            }
        }, 30_000)

        // Request camera permission or start scanning
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 123)
        } else {
            startCamera()
        }
    }

    /**
     * Handles the result of permission request
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /**
     * Starts camera preview and sets up barcode image analyzer
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Analyzes the camera frame and scans for barcodes
     * Accepts result only if it falls within the central scan area
     */
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (processingBarcode) {
            imageProxy.close()
            return
        }

        // Check if overlayRect has been initialized
        if (!::overlayRect.isInitialized) {
            imageProxy.close()
            processingBarcode = false
            return
        }

        processingBarcode = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val viewWidth = previewView.width
                    val viewHeight = previewView.height

                    // Convert image coords to screen coords (rotated 90 deg!)
                    val scaleX = viewWidth.toFloat() / image.height
                    val scaleY = viewHeight.toFloat() / image.width

                    for (barcode in barcodes) {
                        val box = barcode.boundingBox
                        if (!barcode.rawValue.isNullOrBlank() && box != null) {
                            val mappedBox = RectF(
                                box.left * scaleX,
                                box.top * scaleY,
                                box.right * scaleX,
                                box.bottom * scaleY
                            )
                            if (overlayRect.contains(
                                    mappedBox.centerX().toInt(),
                                    mappedBox.centerY().toInt()
                                )
                            ) {
                                val resultIntent = intent
                                    .putExtra("SCAN_RESULT", barcode.rawValue)
                                    .putExtra("SCAN_FORMAT", barcode.format)
                                setResult(RESULT_OK, resultIntent)
                                finish()
                                break
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.scan_failed), Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    processingBarcode = false
                }
        } else {
            imageProxy.close()
            processingBarcode = false
        }
    }
}
