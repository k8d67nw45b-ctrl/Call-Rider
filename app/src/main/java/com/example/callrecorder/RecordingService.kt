package com.example.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private lateinit var audioManager: AudioManager
    private var wasSpeakerOnBefore = false

    companion object {
        private const val TAG = "RecordingService"
        private const val CHANNEL_ID = "call_recording_channel"
        private const val NOTIFICATION_ID = 101
        const val ACTION_START = "com.example.callrecorder.START"
        const val ACTION_STOP = "com.example.callrecorder.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRecording()
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (recorder != null) return

        val dir = File(getExternalFilesDir(null), "CallRecordings")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        outputFile = File(dir, "call_$timestamp.m4a")

        wasSpeakerOnBefore = audioManager.isSpeakerphoneOn
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not enable speakerphone: ${e.message}")
        }

        // Try VOICE_COMMUNICATION first (sometimes captures call audio better
        // than plain MIC on certain devices). If it fails to even initialize,
        // fall back to plain MIC so recording still works outside calls.
        val sourcesToTry = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sourcesToTry) {
            try {
                recorder = MediaRecorder().apply {
                    setAudioSource(source)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile!!.absolutePath)
                    prepare()
                    start()
                }
                Log.i(TAG, "Recording started with source $source: ${outputFile?.absolutePath}")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Source $source failed: ${e.message}")
                recorder?.release()
                recorder = null
            }
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping recorder: ${e.message}")
        } finally {
            recorder = null
        }

        try {
            audioManager.isSpeakerphoneOn = wasSpeakerOnBefore
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore audio mode: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
