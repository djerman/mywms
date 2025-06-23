package rs.djerman.losmobileview

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var settingsButton: Button
    private lateinit var barcodeButton: Button

    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>
    private lateinit var allowedBaseUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)
        allowedBaseUrl = sharedPref.getString("server_url", null) ?: ""

        if (allowedBaseUrl.isBlank()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        settingsButton = findViewById(R.id.btn_settings)
        barcodeButton = findViewById(R.id.btn_barcode)

        // Secure WebView configuration
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val requestedUrl = request.url.toString()
                // Only allow navigation within the originally configured server URL
                return !requestedUrl.startsWith(allowedBaseUrl)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.loadUrl(allowedBaseUrl)

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Handle barcode result and inject into focused element
        barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    val js = "document.activeElement.value = '${it.replace("'", "\\'")}';"
                    webView.evaluateJavascript(js, null)
                }
            }
        }

        barcodeButton.setOnClickListener {
            val scannerPackage = sharedPref.getString("scanner_package", null)
            try {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                if (!scannerPackage.isNullOrBlank()) {
                    intent.setPackage(scannerPackage)
                }
                barcodeLauncher.launch(intent)
            } catch (e: Exception) {
                val playIntent = Intent(Intent.ACTION_VIEW)
                playIntent.data = "market://search?q=barcode+scanner&c=apps".toUri()
                startActivity(playIntent)
            }
        }
    }
}
