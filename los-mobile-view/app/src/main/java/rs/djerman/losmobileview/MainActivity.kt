package rs.djerman.losmobileview

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var settingsButton: Button
    private lateinit var barcodeButton: Button
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var errorRetry: Button
    private lateinit var allowedBaseUrl: String
    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load preferences
        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)
        allowedBaseUrl = sharedPref.getString("server_url", null) ?: ""

        // If URL is missing, redirect to settings screen
        if (allowedBaseUrl.isBlank()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Initialize UI elements
        webView = findViewById(R.id.webview)
        settingsButton = findViewById(R.id.btn_settings)
        barcodeButton = findViewById(R.id.btn_barcode)
        errorLayout = findViewById(R.id.errorLayout)
        errorText = findViewById(R.id.errorText)
        errorRetry = findViewById(R.id.errorRetry)

        // Retry loading on button press
        errorRetry.setOnClickListener {
            if (isNetworkAvailable(this)) {
                showWebView()
                webView.postDelayed({
                    Log.d("WEBVIEW", "Retry loading: $allowedBaseUrl")
                    webView.loadUrl(allowedBaseUrl)
                }, 400)
            } else {
                showErrorMessage(getString(R.string.no_internet))
            }
        }

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val requestedUrl = request.url.toString()
                return !requestedUrl.startsWith(allowedBaseUrl)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                showErrorMessage(getString(R.string.connection_error))
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                showErrorMessage(getString(R.string.connection_error))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WEBVIEW", "onPageStarted: $url")
            }
        }

        // Load page or show network error
        if (isNetworkAvailable(this)) {
            showWebView()
            Log.d("WEBVIEW", "Initial load: $allowedBaseUrl")
            webView.loadUrl(allowedBaseUrl)
        } else {
            showErrorMessage(getString(R.string.no_internet))
        }

        // Open settings screen
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Barcode result handler
        barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    val js = """
                        (function() {
                            var ae = document.activeElement;
                            if (ae && (ae.tagName === 'INPUT' || ae.tagName === 'TEXTAREA') && !ae.readOnly && !ae.disabled && (ae.type === 'text' || ae.type === 'search' || ae.type === 'number' || ae.type === 'tel')) {
                                ae.value = '${it.replace("'", "\\'")}';
                            }
                        })();
                    """.trimIndent()
                    webView.evaluateJavascript(js, null)
                }

                val scannerPackage = result.data?.component?.packageName
                if (!scannerPackage.isNullOrBlank()) {
                    sharedPref.edit().putString("scanner_package", scannerPackage).apply()
                    Log.d("SCANNER", "Saved scanner package: $scannerPackage")
                }
            }
        }

        // Launch scanner or install if missing
        barcodeButton.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)
            barcodeLauncher.launch(intent)
            /*val scannerPackage = sharedPref.getString("scanner_package", null)
            val intent = Intent("com.google.zxing.client.android.SCAN")

            if (!scannerPackage.isNullOrBlank()) {
                intent.setPackage(scannerPackage)
            }

            try {
                barcodeLauncher.launch(intent)
            } catch (e: Exception) {
                sharedPref.edit().remove("scanner_package").apply()
                Toast.makeText(this, getString(R.string.failed_to_launch_scanner), Toast.LENGTH_SHORT).show()
                openPlayStore()
            }*/
        }
    }

    private fun openPlayStore() {
        val playIntent = Intent(Intent.ACTION_VIEW)
        playIntent.data = "market://search?q=barcode+scanner&c=apps".toUri()
        startActivity(playIntent)
    }

    override fun onResume() {
        super.onResume()
        Log.d("WEBVIEW", "onResume: current URL = ${webView.url}")
        if (webView.visibility != View.VISIBLE || webView.url.isNullOrBlank() || webView.url == "about:blank") {
            if (isNetworkAvailable(this)) {
                showWebView()
                Log.d("WEBVIEW", "Resumed load: $allowedBaseUrl")
                webView.loadUrl(allowedBaseUrl)
            } else {
                showErrorMessage(getString(R.string.no_internet))
            }
        } else {
            Log.d("WEBVIEW", "WebView already loaded, skipping reload")
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } catch (e: SecurityException) {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun showErrorMessage(message: String) {
        webView.visibility = android.view.View.GONE
        errorLayout.visibility = android.view.View.VISIBLE
        errorText.text = message
    }

    private fun showWebView() {
        webView.visibility = android.view.View.VISIBLE
        errorLayout.visibility = android.view.View.GONE
    }
}
