package com.example.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        var wasRinging = false
        var callInProgress = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs: SharedPreferences = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val autoRecordEnabled = prefs.getBoolean(Prefs.KEY_AUTO_RECORD, false)
        if (!autoRecordEnabled) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (!callInProgress) {
                    callInProgress = true
                    val serviceIntent = Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_START
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (callInProgress) {
                    callInProgress = false
                    wasRinging = false
                    val serviceIntent = Intent(context, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_STOP
                    }
                    context.startService(serviceIntent)
                }
            }
        }
    }
}

object Prefs {
    const val NAME = "call_recorder_prefs"
    const val KEY_AUTO_RECORD = "auto_record_enabled"
}
