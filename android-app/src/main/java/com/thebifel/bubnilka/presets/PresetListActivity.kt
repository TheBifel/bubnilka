package com.thebifel.bubnilka.presets

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.thebifel.bubnilka.R
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.storage.StylePresetRepository

class PresetListActivity : Activity() {
    private val repository by lazy { StylePresetRepository.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preset_list)
        findViewById<ImageButton>(R.id.preset_list_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        renderPresets()
    }

    private fun renderPresets() {
        val container = findViewById<LinearLayout>(R.id.preset_list_container)
        container.removeAllViews()
        repository.load().forEach { preset -> container.addView(presetRow(preset)) }
        container.addView(
            Button(this).apply {
                setText(R.string.add_symbol)
                textSize = 26f
                isAllCaps = false
                setTextColor(getColor(R.color.bubnilka_text))
                setBackgroundResource(R.drawable.secondary_button)
                contentDescription = getString(R.string.add_preset)
                setOnClickListener { openEditor(null) }
                layoutParams = rowLayoutParams()
            },
        )
    }

    private fun presetRow(preset: StylePreset): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(8), dp(8), dp(8))
        setBackgroundResource(R.drawable.transcript_panel)
        layoutParams = rowLayoutParams()

        addView(
            TextView(context).apply {
                text = preset.title
                textSize = 17f
                setTextColor(getColor(R.color.bubnilka_text))
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        addView(actionButton(R.drawable.ic_edit, R.string.edit_preset) { openEditor(preset.id) })
        addView(actionButton(R.drawable.ic_delete, R.string.delete_preset) { confirmDelete(preset) })
    }

    private fun actionButton(icon: Int, description: Int, action: () -> Unit): ImageButton =
        ImageButton(this).apply {
            setImageResource(icon)
            setBackgroundResource(R.drawable.utility_button)
            contentDescription = getString(description)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                marginStart = dp(8)
            }
        }

    private fun confirmDelete(preset: StylePreset) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_preset_title)
            .setMessage(getString(R.string.delete_preset_message, preset.title))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_preset) { _, _ ->
                if (repository.delete(preset.id)) {
                    renderPresets()
                } else {
                    Toast.makeText(this, R.string.cannot_delete_last_preset, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun openEditor(id: String?) {
        startActivity(
            Intent(this, PresetEditActivity::class.java).apply {
                id?.let { putExtra(PresetEditActivity.EXTRA_PRESET_ID, it) }
            },
        )
    }

    private fun rowLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
