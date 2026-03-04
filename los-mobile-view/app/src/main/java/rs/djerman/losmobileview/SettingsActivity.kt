package rs.djerman.losmobileview

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Build
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_INITIAL_SETUP = "initial_setup"
    }

    private lateinit var radioHttp: RadioButton
    private lateinit var radioHttps: RadioButton
    private lateinit var inputIp: EditText
    private lateinit var inputPort: EditText
    private lateinit var saveBtn: Button
    private lateinit var testConnectionBtn: Button
    private lateinit var testConnectionProgress: ProgressBar
    private lateinit var sharedPref: SharedPreferences
    private var initialSetup: Boolean = false
    @Volatile
    private var isTestingConnection: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initialSetup = intent.getBooleanExtra(EXTRA_INITIAL_SETUP, false)

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

        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)

        // Bind UI elements
        radioHttp = findViewById(R.id.radio_http)
        radioHttps = findViewById(R.id.radio_https)
        inputIp = findViewById(R.id.input_ip)
        inputPort = findViewById(R.id.input_port)
        saveBtn = findViewById(R.id.btn_save)
        testConnectionBtn = findViewById(R.id.btn_test_connection)
        testConnectionProgress = findViewById(R.id.progress_test_connection)

        // Load saved values
        val savedUrl = sharedPref.getString("server_url", "") ?: ""

        if (savedUrl.startsWith("https://")) {
            radioHttps.isChecked = true
        } else {
            radioHttp.isChecked = true
        }

        val urlWithoutPrefix = savedUrl.removePrefix("http://").removePrefix("https://")
        val parts = urlWithoutPrefix.split(":")
        if (parts.size >= 2) {
            inputIp.setText(parts[0])
            val portPart = parts[1].split("/").firstOrNull() ?: ""
            inputPort.setText(portPart)
        }

        // Save button logic
        saveBtn.setOnClickListener {
            val finalUrl = buildServerUrlFromInput() ?: return@setOnClickListener

            sharedPref.edit().apply {
                putString("server_url", finalUrl)
                apply()
            }

            if (initialSetup) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                finish()
                return@setOnClickListener
            }

            setResult(RESULT_OK, Intent().putExtra("server_url", finalUrl))
            finish()
        }

        testConnectionBtn.setOnClickListener {
            val finalUrl = buildServerUrlFromInput() ?: return@setOnClickListener
            testServerConnection(finalUrl)
        }
    }

    private fun buildServerUrlFromInput(): String? {
        val protocol = if (radioHttps.isChecked) "https" else "http"
        val ip = inputIp.text.toString().trim()
        val port = inputPort.text.toString().trim()

        if (ip.isBlank()) {
            inputIp.error = getString(R.string.ip_or_domain_hint)
            return null
        }

        val portNumber = port.toIntOrNull()
        if (port.isBlank() || portNumber == null || portNumber !in 1..65535) {
            inputPort.error = getString(R.string.invalid_port)
            return null
        }

        return "$protocol://$ip:$port/los-mobile"
    }

    private fun testServerConnection(serverUrl: String) {
        if (isTestingConnection) {
            return
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_LONG).show()
            return
        }

        setTestingUiState(true)

        Thread {
            val resultMessage = performConnectionTest(serverUrl)
            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }
                setTestingUiState(false)
                Toast.makeText(this, resultMessage, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun performConnectionTest(serverUrl: String): String {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(serverUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                instanceFollowRedirects = false
            }
            val statusCode = connection.responseCode
            if (statusCode in 200..499) {
                getString(R.string.connection_test_success_with_status, statusCode)
            } else {
                getString(
                    R.string.connection_test_failed_with_reason,
                    getString(R.string.connection_error_http_status, statusCode)
                )
            }
        } catch (_: SocketTimeoutException) {
            getString(
                R.string.connection_test_failed_with_reason,
                getString(R.string.connection_timeout)
            )
        } catch (_: UnknownHostException) {
            getString(
                R.string.connection_test_failed_with_reason,
                getString(R.string.connection_error_host_lookup)
            )
        } catch (_: SSLException) {
            getString(
                R.string.connection_test_failed_with_reason,
                getString(R.string.connection_error_ssl)
            )
        } catch (_: IOException) {
            getString(
                R.string.connection_test_failed_with_reason,
                getString(R.string.connection_error_connect)
            )
        } catch (_: Exception) {
            getString(R.string.connection_test_failed_generic)
        } finally {
            connection?.disconnect()
        }
    }

    private fun setTestingUiState(testing: Boolean) {
        isTestingConnection = testing
        testConnectionBtn.isEnabled = !testing
        saveBtn.isEnabled = !testing
        testConnectionProgress.visibility = if (testing) View.VISIBLE else View.GONE
        testConnectionBtn.text =
            if (testing) getString(R.string.testing_connection) else getString(R.string.test_connection)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } catch (_: SecurityException) {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTestingConnection) {
            setTestingUiState(false)
        }
    }
}
