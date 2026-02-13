package com.medicinereminder

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmActivity : AppCompatActivity() {
    
    private var taskId: String = ""
    private var taskName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        setContentView(R.layout.activity_alarm)
        
        taskId = intent.getStringExtra("TASK_ID") ?: ""
        taskName = intent.getStringExtra("TASK_NAME") ?: ""
        
        Log.d("AlarmActivity", "Showing alarm for: $taskName")
        
        setupUI()
    }
    
    private fun setupUI() {
        findViewById<TextView>(R.id.taskNameText).text = taskName
        
        findViewById<Button>(R.id.btnYes).setOnClickListener {
            Log.d("AlarmActivity", "YES clicked for: $taskName")
            markTaskComplete()
            dismissAlarm()
        }
        
        findViewById<Button>(R.id.btnRemindAgain).setOnClickListener {
            Log.d("AlarmActivity", "REMIND AGAIN clicked for: $taskName")
            snoozeAlarm()
            dismissAlarm()
        }
    }
    
    private fun markTaskComplete() {
        val tasksJson = AsyncStorageHelper.getTasksJson(this)

        try {
            val jsonArray = JSONArray(tasksJson)

            for (i in 0 until jsonArray.length()) {
                val task = jsonArray.getJSONObject(i)
                if (task.getString("id") == taskId) {
                    val isRecurring = task.optBoolean("isRecurring", false)

                    if (isRecurring) {
                        // Reschedule for tomorrow
                        val calendar = Calendar.getInstance()
                        val scheduledTime = task.getString("scheduledTime")
                        val originalTime = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            Locale.US
                        ).parse(scheduledTime)

                        if (originalTime != null) {
                            calendar.time = originalTime
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(Calendar.MINUTE)

                            // Set to tomorrow at same time
                            val tomorrow = Calendar.getInstance()
                            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                            tomorrow.set(Calendar.HOUR_OF_DAY, hour)
                            tomorrow.set(Calendar.MINUTE, minute)
                            tomorrow.set(Calendar.SECOND, 0)

                            // Update task time in AsyncStorage
                            task.put("scheduledTime", SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.US
                            ).format(tomorrow.time))

                            AsyncStorageHelper.saveTasksJson(this, jsonArray.toString())

                            // Reschedule alarm
                            val scheduler = AlarmScheduler(this)
                            scheduler.scheduleAlarm(taskId, taskName, tomorrow.timeInMillis)

                            Log.d("AlarmActivity", "Daily alarm rescheduled for tomorrow: ${tomorrow.time}")
                        }
                    } else {
                        // One-time alarm - mark as completed
                        task.put("completed", true)
                        task.put("completedAt", SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            Locale.US
                        ).format(Date()))

                        AsyncStorageHelper.saveTasksJson(this, jsonArray.toString())
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "Error handling task completion", e)
        }

        // Send event to React Native to refresh UI
        AlarmModule.sendTaskCompletedEvent(taskId)
    }
    
    private fun snoozeAlarm() {
        val prefs = getSharedPreferences("TaskPreferences", Context.MODE_PRIVATE)
        val snoozeDuration = prefs.getInt("snooze_$taskId", 10).toLong()
        
        val newTriggerTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000)
        
        val scheduler = AlarmScheduler(this)
        scheduler.scheduleAlarm(taskId, taskName, newTriggerTime)
        
        AlarmModule.sendTaskSnoozedEvent(taskId, newTriggerTime)
    }
    
    private fun dismissAlarm() {
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
        finish()
    }
    
    override fun onBackPressed() {
        // Prevent dismissing with back button
    }
}