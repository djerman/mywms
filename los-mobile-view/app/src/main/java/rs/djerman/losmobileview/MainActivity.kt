package rs.djerman.losmobileview

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

/**
 * MainActivity
 * Displays the configured myWMS mobile page in a WebView.
 * Handles barcode scanning and network errors.
 */
class MainActivity : AppCompatActivity() {

    // Persistent settings storage
    private lateinit var sharedPref: SharedPreferences

    // UI components
    private lateinit var webView: WebView
    private lateinit var settingsButton: Button
    private lateinit var barcodeButton: Button
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var errorRetry: Button

    // Barcode scanner launcher
    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>

    // Server base URL allowed for WebView
    private lateinit var allowedBaseUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load server URL from saved preferences
        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)
        allowedBaseUrl = sharedPref.getString("server_url", null) ?: ""

        // If no URL set, go to settings screen
        if (allowedBaseUrl.isBlank()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Initialize UI references
        webView = findViewById(R.id.webview)
        settingsButton = findViewById(R.id.btn_settings)
        barcodeButton = findViewById(R.id.btn_barcode)
        errorLayout = findViewById(R.id.errorLayout)
        errorText = findViewById(R.id.errorText)
        errorRetry = findViewById(R.id.errorRetry)

        // Retry button reloads the page after checking network
        errorRetry.setOnClickListener {
            if (isNetworkAvailable(this)) {
                showWebView()
                // Delay the URL loading slightly to avoid premature error
                webView.postDelayed({
                    webView.loadUrl(allowedBaseUrl)
                }, 400)
            } else {
                showErrorMessage(getString(R.string.no_internet))
            }
        }

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            // Allow only URLs starting with configured base URL
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val requestedUrl = request.url.toString()
                return !requestedUrl.startsWith(allowedBaseUrl)
            }

            // Show error on generic load failure
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                showErrorMessage(getString(R.string.connection_error))
            }

            // Show error on HTTP error (e.g. 404, 500)
            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                showErrorMessage(getString(R.string.connection_error))
            }
        }

        // Load the initial URL if network is available
        if (isNetworkAvailable(this)) {
            showWebView()
            webView.loadUrl(allowedBaseUrl)
        } else {
            showErrorMessage(getString(R.string.no_internet))
        }

        // Open settings screen
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Handle result from barcode scanner app
        barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    // Insert scanned value into active input field
                    val js = "document.activeElement.value = '${it.replace("'", "\\'")}';"
                    webView.evaluateJavascript(js, null)
                }
            }
        }

        // Trigger external barcode scanning app
        barcodeButton.setOnClickListener {
            val scannerPackage = sharedPref.getString("scanner_package", null)
            try {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                if (!scannerPackage.isNullOrBlank()) {
                    intent.setPackage(scannerPackage)
                }
                barcodeLauncher.launch(intent)
            } catch (e: Exception) {
                // If scanner not installed, open Play Store
                val playIntent = Intent(Intent.ACTION_VIEW)
                playIntent.data = "market://search?q=barcode+scanner&c=apps".toUri()
                startActivity(playIntent)
            }
        }
    }

    /**
     * Check and refresh connectivity on resume
     */
    override fun onResume() {
        super.onResume()
        if (isNetworkAvailable(this)) {
            showWebView()
            webView.loadUrl(allowedBaseUrl)
        } else {
            showErrorMessage(getString(R.string.no_internet))
        }
    }

    /**
     * Checks for active internet connectivity
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val network = connectivityManager.activeNetwork
                if (network != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                } else {
                    false
                }
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

    /**
     * Displays an error message and hides the WebView
     */
    private fun showErrorMessage(message: String) {
        webView.visibility = android.view.View.GONE
        errorLayout.visibility = android.view.View.VISIBLE
        errorText.text = message
    }

    /**
     * Shows the WebView and hides the error message layout
     */
    private fun showWebView() {
        webView.visibility = android.view.View.VISIBLE
        errorLayout.visibility = android.view.View.GONE
    }
}
