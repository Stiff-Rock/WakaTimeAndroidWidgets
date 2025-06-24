package com.stiffrock.wakatimewidgets.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stiffrock.wakatimewidgets.R
import com.stiffrock.wakatimewidgets.data.WakaTimeApi
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var prefsFileName = "user_prefs"
    private var apiKeyPref = "user_api_key"

    private lateinit var currentUserTextView: TextView
    private lateinit var wakaTimeApiKeyInput: EditText
    private lateinit var saveBtn: Button

    //TODO: MAYBE DO OAuth 2.0
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        currentUserTextView = findViewById(R.id.currentUser)
        wakaTimeApiKeyInput = findViewById(R.id.wakaTimeApiKeyInput)
        saveBtn = findViewById(R.id.saveBtn)

        sharedPreferences =
            this.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)

        val savedApiKey = sharedPreferences.getString(apiKeyPref, null)

        if (savedApiKey != null) {
            fetchUserData(savedApiKey, false)
            wakaTimeApiKeyInput.setText(savedApiKey)
        }

        saveBtn.setOnClickListener {
            val apiKey = wakaTimeApiKeyInput.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Please provide your WakaTime API key", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            fetchUserData(apiKey, true)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchUserData(apiKey: String, save: Boolean) {
        lifecycleScope.launch {
            try {
                val formattedKey = WakaTimeApi.formatApiKey(apiKey)
                val response = WakaTimeApi.service.getCurrentUser(formattedKey)

                if (response.isSuccessful) {
                    val username = response.body()?.data?.username

                    if (save){
                        sharedPreferences.edit().apply {
                            putString(apiKeyPref, apiKey)
                            apply()
                        }
                    }

                    if (username != null) {
                        currentUserTextView.text = username
                        Toast.makeText(
                            this@MainActivity,
                            "Successfully saved API key",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity, "Failed to get username", Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}