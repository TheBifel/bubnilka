package com.thebifel.bubnilka

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.thebifel.bubnilka.storage.ConsentRepository

class MainActivity : Activity() {
    private lateinit var consentRepository: ConsentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        consentRepository = ConsentRepository.from(this)

        findViewById<CheckBox>(R.id.cloud_consent_checkbox).setOnCheckedChangeListener { _, accepted ->
            if (accepted) consentRepository.accept() else consentRepository.revoke()
            renderConsent()
        }

        findViewById<Button>(R.id.grant_microphone_button).setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_REQUEST)
        }
        findViewById<Button>(R.id.enable_keyboard_button).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.select_keyboard_button).setOnClickListener {
            getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        renderMicrophonePermission()
        renderConsent()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_REQUEST) renderMicrophonePermission()
    }

    private fun renderMicrophonePermission() {
        val granted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.permission_status).setText(
            if (granted) R.string.microphone_granted else R.string.microphone_not_granted,
        )
    }

    private fun renderConsent() {
        val accepted = consentRepository.isAccepted()
        val checkbox = findViewById<CheckBox>(R.id.cloud_consent_checkbox)
        if (checkbox.isChecked != accepted) checkbox.isChecked = accepted
        findViewById<TextView>(R.id.consent_status).setText(
            if (accepted) R.string.consent_accepted else R.string.consent_required,
        )
    }

    private companion object {
        const val MICROPHONE_REQUEST = 100
    }
}
