package rs.djerman.losmobileview

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var settingsButton: Button
    private lateinit var barcodeButton: Button
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var errorRetry: Button
    private lateinit var loadingLayout: LinearLayout
    private lateinit var loadingText: TextView
    private var allowedBaseUrl: String = ""
    private lateinit var barcodeLauncher: ActivityResultLauncher<Intent>
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>
    private val loadTimeoutMs = 10_000L
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var hasMainFrameError = false
    private var currentLoadId = 0
    private var activeLoadId = 0
    private var isMainFrameLoading = false
    private var pendingScanPayload: ParsedScanPayload? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load preferences
        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)
        allowedBaseUrl = getSavedServerUrl()

        settingsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val newUrl = getSavedServerUrl()
                    if (newUrl.isBlank()) {
                        showErrorMessage(getString(R.string.connection_error))
                        return@registerForActivityResult
                    }
                    if (newUrl != allowedBaseUrl) {
                        allowedBaseUrl = newUrl
                        resetWebViewForServerChange()
                    }
                    loadConfiguredServer()
                }
            }

        // If URL is missing, redirect to settings screen
        if (allowedBaseUrl.isBlank()) {
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .putExtra(SettingsActivity.EXTRA_INITIAL_SETUP, true)
            )
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        enableEdgeToEdge()

        val root = findViewById<View>(R.id.root)  // корен из XML-а
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

        // Initialize UI elements
        webView = findViewById(R.id.webview)
        settingsButton = findViewById(R.id.btn_settings)
        barcodeButton = findViewById(R.id.btn_barcode)
        errorLayout = findViewById(R.id.errorLayout)
        errorText = findViewById(R.id.errorText)
        errorRetry = findViewById(R.id.errorRetry)
        loadingLayout = findViewById(R.id.loadingLayout)
        loadingText = findViewById(R.id.loadingText)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true

        // Retry loading on button press
        errorRetry.setOnClickListener {
            loadConfiguredServer()
        }

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.textZoom = 100

        // Use modern Chrome-style user agent to avoid server rejection
        webView.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (!request.isForMainFrame) {
                    return false
                }
                return !isAllowedNavigation(request.url)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame && request.url.toString() != "about:blank") {
                    hasMainFrameError = true
                    isMainFrameLoading = false
                    view.stopLoading()
                    showErrorMessage(messageForWebError(error))
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    hasMainFrameError = true
                    isMainFrameLoading = false
                    view.stopLoading()
                    showErrorMessage(
                        getString(
                            R.string.connection_error_http_status,
                            errorResponse.statusCode
                        )
                    )
                }
            }

            // Handles SSL certificate errors (invalid HTTPS) and shows fallback UI instead of crashing
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                Log.e("WEBVIEW", "SSL Error: ${error.primaryError}")
                hasMainFrameError = true
                isMainFrameLoading = false
                handler.cancel() // Do not proceed for invalid certificates
                showErrorMessage(getString(R.string.connection_error_ssl))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d("WEBVIEW", "onPageStarted: $url")
                if (!url.isNullOrBlank() && url != "about:blank") {
                    hasMainFrameError = false
                    isMainFrameLoading = true
                    showLoading(getString(R.string.loading))
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url.isNullOrBlank() || url == "about:blank") {
                    return
                }
                cancelLoadTimeout()
                isMainFrameLoading = false
                if (hasMainFrameError) {
                    return
                }
                if (isAllowedNavigation(url.toUri())) {
                    showWebView()
                    installScanSupport()
                    flushPendingScan()
                }
            }
        }

        // Load page or show network error
        loadConfiguredServer()

        // Open settings screen
        settingsButton.setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }

        // Barcode result handler
        barcodeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let { rawValue ->
                    val scanFormat = result.data?.getIntExtra("SCAN_FORMAT", -1) ?: -1
                    val parsed = ScanPayloadParser.parse(
                        rawValue = rawValue,
                        symbologyLabel = describeScanFormat(scanFormat),
                    )
                    deliverScanPayload(parsed)
                }

                val scannerPackage = result.data?.component?.packageName
                if (!scannerPackage.isNullOrBlank()) {
                    sharedPref.edit().putString("scanner_package", scannerPackage).apply()
                    Log.d("SCANNER", "Saved scanner package: $scannerPackage")
                }
            }
        }

        // Launch scanner or install if missing
        @androidx.camera.core.ExperimentalGetImage
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

        handleScannerIntent(intent)
    }

    private fun openPlayStore() {
        val playIntent = Intent(Intent.ACTION_VIEW)
        playIntent.data = "market://search?q=barcode+scanner&c=apps".toUri()
        startActivity(playIntent)
    }

    override fun onResume() {
        super.onResume()
        if (!::webView.isInitialized) {
            return
        }

        val latestServerUrl = getSavedServerUrl()
        if (latestServerUrl.isNotBlank() && latestServerUrl != allowedBaseUrl) {
            allowedBaseUrl = latestServerUrl
            resetWebViewForServerChange()
            loadConfiguredServer()
            return
        }

        Log.d("WEBVIEW", "onResume: current URL = ${webView.url}")
        if (isMainFrameLoading || loadingLayout.visibility == View.VISIBLE) {
            Log.d("WEBVIEW", "Main frame already loading, skipping duplicate reload")
            return
        }
        if (webView.visibility != View.VISIBLE || webView.url.isNullOrBlank() || webView.url == "about:blank") {
            loadConfiguredServer()
        } else {
            Log.d("WEBVIEW", "WebView already loaded, skipping reload")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScannerIntent(intent)
    }

    private fun handleScannerIntent(intent: Intent?) {
        val rawValue = scanDataExtraKeys.firstNotNullOfOrNull { key -> intent?.getStringExtra(key)?.trim() }
            ?.takeIf { it.isNotBlank() }
            ?: return
        val symbologyLabel = scanLabelExtraKeys.firstNotNullOfOrNull { key -> intent?.getStringExtra(key)?.trim() }
        val parsed = ScanPayloadParser.parse(rawValue, symbologyLabel)
        deliverScanPayload(parsed)
    }

    private fun deliverScanPayload(payload: ParsedScanPayload) {
        if (!::webView.isInitialized || webView.url.isNullOrBlank() || webView.url == "about:blank") {
            pendingScanPayload = payload
            return
        }

        pendingScanPayload = null
        webView.evaluateJavascript(buildApplyScanJavascript(payload), null)
    }

    private fun flushPendingScan() {
        pendingScanPayload?.let { payload ->
            pendingScanPayload = null
            webView.evaluateJavascript(buildApplyScanJavascript(payload), null)
        }
    }

    private fun buildApplyScanJavascript(payload: ParsedScanPayload): String {
        val rawValue = JSONObject.quote(payload.rawValue)
        val defaultValue = JSONObject.quote(payload.itemCode ?: payload.rawValue)
        val itemCode = payload.itemCode?.let(JSONObject::quote) ?: "null"
        val lot = payload.lot?.let(JSONObject::quote) ?: "null"
        val expiryDate = payload.expiryDate?.let(JSONObject::quote) ?: "null"
        val serial = payload.serial?.let(JSONObject::quote) ?: "null"

        return """
            (function() {
                function isEditable(el) {
                    if (!el) return false;
                    var tag = (el.tagName || '').toUpperCase();
                    if (tag !== 'INPUT' && tag !== 'TEXTAREA') return false;
                    if (el.readOnly || el.disabled) return false;
                    if (tag === 'TEXTAREA') return true;
                    var type = (el.type || 'text').toLowerCase();
                    return type === 'text' || type === 'search' || type === 'number' || type === 'tel';
                }

                function inferScanTarget(el) {
                    if (!el) return 'raw';
                    var localTarget = el.__losMobileScanTarget;
                    if (localTarget) return String(localTarget).toLowerCase();
                    var explicitTarget = el.getAttribute && el.getAttribute('data-scan-target');
                    if (explicitTarget) return explicitTarget.toLowerCase();
                    var hint = ((el.id || '') + ' ' + (el.name || '')).toLowerCase();
                    if (hint.indexOf('lot') !== -1 || hint.indexOf('charge') !== -1) return 'lot';
                    if (hint.indexOf('validto') !== -1 || hint.indexOf('expiry') !== -1 || hint.indexOf('expire') !== -1) return 'expiry';
                    if (hint.indexOf('serial') !== -1) return 'serial';
                    if (hint.indexOf('item') !== -1 || hint.indexOf('article') !== -1 || hint.indexOf('articel') !== -1 || hint.indexOf('stockid') !== -1 || hint.indexOf('material') !== -1 || hint.indexOf('mat') !== -1) return 'item';
                    return 'raw';
                }

                function firstEditableField() {
                    var fields = document.querySelectorAll('input:not([type=hidden]):not([disabled]), textarea:not([disabled])');
                    for (var i = 0; i < fields.length; i++) {
                        var field = fields[i];
                        if (!isEditable(field)) continue;
                        if (field.offsetParent === null) continue;
                        return field;
                    }
                    return null;
                }

                if (typeof window.__losMobileRefreshFocus === 'function') {
                    window.__losMobileRefreshFocus();
                }

                var ae = document.activeElement;
                if (!isEditable(ae)) {
                    ae = firstEditableField();
                }
                if (!isEditable(ae)) {
                    return;
                }

                var target = inferScanTarget(ae);
                var values = {
                    item: $itemCode,
                    lot: $lot,
                    expiry: $expiryDate,
                    serial: $serial,
                    raw: $rawValue
                };
                var chosen = values[target];
                if (chosen == null || chosen === '') {
                    chosen = $defaultValue;
                }

                ae.focus();
                ae.value = chosen;
                if (typeof ae.select === 'function') {
                    ae.select();
                }
                if (typeof Event === 'function') {
                    ae.dispatchEvent(new Event('input', { bubbles: true }));
                    ae.dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """.trimIndent()
    }

    private fun installScanSupport() {
        if (!::webView.isInitialized) {
            return
        }
        webView.requestFocus()
        webView.requestFocus(View.FOCUS_DOWN)
        webView.post {
            webView.evaluateJavascript(scanSupportJavascript, null)
        }
    }

    private fun getSavedServerUrl(): String {
        return sharedPref.getString("server_url", "")?.trim() ?: ""
    }

    private fun loadConfiguredServer() {
        if (allowedBaseUrl.isBlank()) {
            showErrorMessage(getString(R.string.connection_error))
            return
        }
        if (!isNetworkAvailable(this)) {
            showErrorMessage(getString(R.string.no_internet))
            return
        }
        if (isMainFrameLoading) {
            Log.d("WEBVIEW", "loadConfiguredServer skipped: request already in progress")
            return
        }
        hasMainFrameError = false
        isMainFrameLoading = true
        activeLoadId = ++currentLoadId
        showLoading(getString(R.string.loading))
        startLoadTimeout(activeLoadId)
        Log.d("WEBVIEW", "Loading URL: $allowedBaseUrl")
        webView.loadUrl(allowedBaseUrl)
    }

    private fun resetWebViewForServerChange() {
        cancelLoadTimeout()
        hasMainFrameError = false
        isMainFrameLoading = false
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        WebStorage.getInstance().deleteAllData()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun isAllowedNavigation(requestedUri: Uri): Boolean {
        if (requestedUri.toString() == "about:blank") {
            return true
        }

        val scheme = requestedUri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") {
            return false
        }

        val allowedUri = allowedBaseUrl.toUri()
        val allowedHost = allowedUri.host ?: return false
        val requestedHost = requestedUri.host ?: return false

        return requestedUri.scheme.equals(allowedUri.scheme, ignoreCase = true) &&
            requestedHost.equals(allowedHost, ignoreCase = true) &&
            resolvedPort(requestedUri) == resolvedPort(allowedUri)
    }

    private fun resolvedPort(uri: Uri): Int {
        if (uri.port > 0) {
            return uri.port
        }
        return if (uri.scheme.equals("https", ignoreCase = true)) 443 else 80
    }

    private fun messageForWebError(error: WebResourceError): String {
        return when (error.errorCode) {
            WebViewClient.ERROR_TIMEOUT -> getString(R.string.connection_timeout)
            WebViewClient.ERROR_HOST_LOOKUP -> getString(R.string.connection_error_host_lookup)
            WebViewClient.ERROR_CONNECT -> getString(R.string.connection_error_connect)
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> getString(R.string.connection_error_ssl)
            else -> getString(R.string.connection_error)
        }
    }

    private fun startLoadTimeout(loadId: Int) {
        cancelLoadTimeout()
        timeoutHandler.postDelayed({
            if (loadId != activeLoadId || hasMainFrameError) {
                return@postDelayed
            }
            if (::loadingLayout.isInitialized && loadingLayout.visibility == View.VISIBLE) {
                hasMainFrameError = true
                isMainFrameLoading = false
                webView.stopLoading()
                showErrorMessage(getString(R.string.connection_timeout))
            }
        }, loadTimeoutMs)
    }

    private fun cancelLoadTimeout() {
        timeoutHandler.removeCallbacksAndMessages(null)
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
        cancelLoadTimeout()
        hasMainFrameError = true
        isMainFrameLoading = false
        webView.visibility = android.view.View.GONE
        loadingLayout.visibility = android.view.View.GONE
        errorLayout.visibility = android.view.View.VISIBLE
        errorText.text = message
    }

    private fun showLoading(message: String) {
        webView.visibility = android.view.View.GONE
        errorLayout.visibility = android.view.View.GONE
        loadingLayout.visibility = android.view.View.VISIBLE
        loadingText.text = message
    }

    private fun showWebView() {
        cancelLoadTimeout()
        isMainFrameLoading = false
        webView.visibility = android.view.View.VISIBLE
        loadingLayout.visibility = android.view.View.GONE
        errorLayout.visibility = android.view.View.GONE
    }

    companion object {
        private val scanDataExtraKeys = listOf(
            "com.symbol.datawedge.data_string",
            "com.datalogic.decode.intentwedge.barcode_string",
            "com.honeywell.decode.intent.data",
            "com.honeywell.aidc.extra.DATA",
            "data",
            "SCAN_RESULT",
        )

        private val scanLabelExtraKeys = listOf(
            "com.symbol.datawedge.label_type",
            "com.datalogic.decode.intentwedge.barcode_type",
            "com.honeywell.decode.intent.code_id",
            "com.honeywell.decode.intent.aim_id",
            "com.honeywell.aidc.extra.CODE_ID",
            "com.honeywell.aidc.extra.AIM_ID",
            "codeId",
            "aimId",
            "label_type",
            "SCAN_RESULT_FORMAT",
        )

        private fun describeScanFormat(format: Int): String? {
            return when (format) {
                Barcode.FORMAT_QR_CODE -> "QR_CODE"
                Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
                Barcode.FORMAT_PDF417 -> "PDF417"
                Barcode.FORMAT_AZTEC -> "AZTEC"
                Barcode.FORMAT_CODE_128 -> "CODE_128"
                Barcode.FORMAT_CODE_39 -> "CODE_39"
                Barcode.FORMAT_CODE_93 -> "CODE_93"
                Barcode.FORMAT_CODABAR -> "CODABAR"
                Barcode.FORMAT_EAN_13 -> "EAN_13"
                Barcode.FORMAT_EAN_8 -> "EAN_8"
                Barcode.FORMAT_ITF -> "ITF"
                Barcode.FORMAT_UPC_A -> "UPC_A"
                Barcode.FORMAT_UPC_E -> "UPC_E"
                else -> null
            }
        }

        private val scanSupportJavascript = """
            (function() {
                if (window.__losMobileScanBootstrapInstalled) {
                    if (typeof window.__losMobileRefreshFocus === 'function') {
                        window.__losMobileRefreshFocus();
                    }
                    return;
                }

                function isEditable(el) {
                    if (!el) return false;
                    var tag = (el.tagName || '').toUpperCase();
                    if (tag !== 'INPUT' && tag !== 'TEXTAREA') return false;
                    if (el.readOnly || el.disabled) return false;
                    if (tag === 'TEXTAREA') return true;
                    var type = (el.type || 'text').toLowerCase();
                    return type === 'text' || type === 'search' || type === 'number' || type === 'tel';
                }

                function inferScanTarget(el) {
                    if (!el) return 'raw';
                    var localTarget = el.__losMobileScanTarget;
                    if (localTarget) return String(localTarget).toLowerCase();
                    var explicitTarget = el.getAttribute && el.getAttribute('data-scan-target');
                    if (explicitTarget) return explicitTarget.toLowerCase();
                    var hint = ((el.id || '') + ' ' + (el.name || '')).toLowerCase();
                    if (hint.indexOf('lot') !== -1 || hint.indexOf('charge') !== -1) return 'lot';
                    if (hint.indexOf('validto') !== -1 || hint.indexOf('expiry') !== -1 || hint.indexOf('expire') !== -1) return 'expiry';
                    if (hint.indexOf('serial') !== -1) return 'serial';
                    if (hint.indexOf('item') !== -1 || hint.indexOf('article') !== -1 || hint.indexOf('articel') !== -1 || hint.indexOf('stockid') !== -1 || hint.indexOf('material') !== -1 || hint.indexOf('mat') !== -1) return 'item';
                    return 'raw';
                }

                function rememberScanTarget(el) {
                    if (!isEditable(el)) return;
                    window.__losMobileScanTarget = inferScanTarget(el);
                }

                function firstEditableField() {
                    var fields = document.querySelectorAll('input:not([type=hidden]):not([disabled]), textarea:not([disabled])');
                    for (var i = 0; i < fields.length; i++) {
                        var field = fields[i];
                        if (!isEditable(field)) continue;
                        if (field.offsetParent === null) continue;
                        return field;
                    }
                    return null;
                }

                window.__losMobileRefreshFocus = function() {
                    var active = document.activeElement;
                    if (!isEditable(active)) {
                        active = firstEditableField();
                    }
                    if (!isEditable(active)) {
                        return;
                    }
                    active.focus();
                    if (typeof active.select === 'function') {
                        active.select();
                    }
                    rememberScanTarget(active);
                };

                document.addEventListener('focusin', function(event) {
                    rememberScanTarget(event.target);
                }, true);

                window.__losMobileScanBootstrapInstalled = true;
                setTimeout(function() {
                    window.__losMobileRefreshFocus();
                }, 120);
            })();
        """
    }

    override fun onDestroy() {
        cancelLoadTimeout()
        super.onDestroy()
    }
}
