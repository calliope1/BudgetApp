package com.example.budgetapp.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.budgetapp.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etServerUrl = findViewById<EditText>(R.id.etServerUrl)
        val etSecret = findViewById<EditText>(R.id.etSecret)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        val prefs = getSharedPreferences("BudgetPrefs", MODE_PRIVATE)

        etServerUrl.setText(prefs.getString("server_url", "http://example.com:5000"))
        etSecret.setText(prefs.getString("shared_secret", ""))

        btnSave.setOnClickListener {
            val serverUrl = etServerUrl.text.toString().trim()
            val secret = etSecret.text.toString().trim()

            prefs.edit {
                putString("server_url", serverUrl)
                    .putString("shared_secret", secret)
            }

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}
