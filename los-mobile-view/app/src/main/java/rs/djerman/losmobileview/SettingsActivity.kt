package rs.djerman.losmobileview

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var radioHttp: RadioButton
    private lateinit var radioHttps: RadioButton
    private lateinit var inputIp: EditText
    private lateinit var inputPort: EditText
    private lateinit var saveBtn: Button
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPref = getSharedPreferences("los_settings", MODE_PRIVATE)

        // Bind UI elements
        radioHttp = findViewById(R.id.radio_http)
        radioHttps = findViewById(R.id.radio_https)
        inputIp = findViewById(R.id.input_ip)
        inputPort = findViewById(R.id.input_port)
        saveBtn = findViewById(R.id.btn_save)

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
            val protocol = if (radioHttps.isChecked) "https" else "http"
            val ip = inputIp.text.toString().trim()
            val port = inputPort.text.toString().trim()

            if (ip.isBlank()) {
                inputIp.error = getString(R.string.ip_or_domain_hint)
                return@setOnClickListener
            }

            val portNumber = port.toIntOrNull()
            if (port.isBlank() || portNumber == null || portNumber !in 1..65535) {
                inputPort.error = getString(R.string.invalid_port)
                return@setOnClickListener
            }

            val finalUrl = "$protocol://$ip:$port/los-mobile"

            sharedPref.edit().apply {
                putString("server_url", finalUrl)
                apply()
            }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
