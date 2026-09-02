package com.thebifel.bubnilka.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UiDumpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UiInspector.ACTION_DUMP) return
        val output = UiInspector.writeDump(context)
        Log.d("BubnilkaUiInspector", "IME tree written to ${output.name}")
    }
}
