package com.medicinereminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class AlarmService : Service() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val CHANNEL_ID = "alarm_service_channel"
        const val NOTIFICATION_ID = 1001
        var isRunning = false
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DailyReminder::AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)
        
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra("TASK_ID") ?: ""
        val taskName = intent?.getStringExtra("TASK_NAME") ?: ""
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Daily Reminder")
            .setContentText("Alarm: $taskName")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
        
        launchFullScreenAlarm(taskId, taskName)
        startAlarmSound()
        startVibration()
        
        Handler(Looper.getMainLooper()).postDelayed({
            speakTaskName(taskName)
        }, 1500)
        
        return START_STICKY
    }
    
    private fun launchFullScreenAlarm(taskId: String, taskName: String) {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("TASK_ID", taskId)
            putExtra("TASK_NAME", taskName)
        }
        
        startActivity(fullScreenIntent)
    }
    
    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                
                prepare()
                start()
            }
            
            Log.d("AlarmService", "Alarm sound started")
        } catch (e: Exception) {
            Log.e("AlarmService", "Error starting alarm sound", e)
        }
    }
    
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 1000, 500)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(pattern, 0)
            vibrator?.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
    
    private fun speakTaskName(taskName: String) {
    if (tts == null) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                val message = "Did you complete $taskName?"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "AlarmTTS")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(message, TextToSpeech.QUEUE_ADD, null)
                }
            }
        }
    } else {
        val message = "Did you complete $taskName?"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "AlarmTTS")
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(message, TextToSpeech.QUEUE_ADD, null)
        }
    }
    Log.d("AlarmService", "TTS spoken: Did you complete $taskName?")
}
    
    fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        vibrator?.cancel()
        
        tts?.stop()
        tts?.shutdown()
        
        wakeLock?.release()
        
        isRunning = false
        stopForeground(true)
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medicine reminder alarms"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}