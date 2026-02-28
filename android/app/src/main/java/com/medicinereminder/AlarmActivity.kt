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
import java.util.Calendar

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

        Log.d(TAG, "=== AlarmActivity started ===")
        Log.d(TAG, "taskId='$taskId'  taskName='$taskName'")

        setupUI()
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.taskNameText).text = taskName

        findViewById<Button>(R.id.btnYes).setOnClickListener {
            Log.d(TAG, "YES button clicked for: $taskName")
            markTaskComplete()
            dismissAlarm()
        }

        findViewById<Button>(R.id.btnRemindAgain).setOnClickListener {
            Log.d(TAG, "REMIND AGAIN clicked for: $taskName")
            snoozeAlarm()
            dismissAlarm()
        }
    }

    private fun markTaskComplete() {
        Log.d(TAG, "--- markTaskComplete() START ---")

        if (taskId.isEmpty()) {
            Log.e(TAG, "taskId is empty — cannot proceed")
            return
        }

        // Read task scheduling info from SharedPreferences (saved by AlarmModule.scheduleAlarm).
        // This is reliable: no SQLite locking, no timezone parsing, no caching issues.
        val prefs = getSharedPreferences(AlarmModule.PREFS_NAME, Context.MODE_PRIVATE)
        val isRecurring  = prefs.getBoolean("task_recurring_$taskId", false)
        val scheduledHour   = prefs.getInt("task_hour_$taskId", -1)
        val scheduledMinute = prefs.getInt("task_minute_$taskId", -1)

        Log.d(TAG, "SharedPrefs → isRecurring=$isRecurring, hour=$scheduledHour, minute=$scheduledMinute")

        if (isRecurring) {
            Log.d(TAG, "Task is RECURRING — rescheduling for tomorrow")

            if (scheduledHour == -1 || scheduledMinute == -1) {
                // Fallback: SharedPreferences entry is missing (e.g. old install, prefs cleared).
                // Use the current time as the scheduled hour/minute since the alarm just fired now.
                Log.w(TAG, "No SharedPrefs entry found — falling back to current time for hour/minute")
                rescheduleRecurring(Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                                    Calendar.getInstance().get(Calendar.MINUTE))
            } else {
                rescheduleRecurring(scheduledHour, scheduledMinute)
            }
        } else {
            Log.d(TAG, "Task is ONE-TIME — sending completed event to React Native")
            // For one-time tasks: tell React Native to mark completed in AsyncStorage.
            AlarmModule.sendTaskCompletedEvent(taskId)
        }

        Log.d(TAG, "--- markTaskComplete() END ---")
    }

    private fun rescheduleRecurring(hour: Int, minute: Int) {
        // Build tomorrow's Calendar at the same LOCAL hour:minute.
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        tomorrow.set(Calendar.HOUR_OF_DAY, hour)
        tomorrow.set(Calendar.MINUTE, minute)
        tomorrow.set(Calendar.SECOND, 0)
        tomorrow.set(Calendar.MILLISECOND, 0)

        Log.d(TAG, "Rescheduling '$taskName' for: ${tomorrow.time}  (millis=${tomorrow.timeInMillis})")

        val scheduler = AlarmScheduler(this)
        val success = scheduler.scheduleAlarm(taskId, taskName, tomorrow.timeInMillis)

        Log.d(TAG, "AlarmScheduler.scheduleAlarm() returned: $success")

        if (success) {
            // Tell React Native the task has been rescheduled so it can update AsyncStorage.
            AlarmModule.sendTaskRescheduledEvent(taskId, tomorrow.timeInMillis)
            Log.d(TAG, "onTaskRescheduled event sent")
        } else {
            Log.e(TAG, "scheduleAlarm FAILED — recurring alarm will NOT fire tomorrow!")
            // Still notify React Native so the UI can refresh.
            AlarmModule.sendTaskRescheduledEvent(taskId, tomorrow.timeInMillis)
        }
    }

    private fun snoozeAlarm() {
        val prefs = getSharedPreferences("TaskPreferences", Context.MODE_PRIVATE)
        val snoozeDuration = prefs.getInt("snooze_$taskId", 10).toLong()

        val newTriggerTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000)
        Log.d(TAG, "Snoozing '$taskName' for ${snoozeDuration} min → new trigger: $newTriggerTime")

        val scheduler = AlarmScheduler(this)
        scheduler.scheduleAlarm(taskId, taskName, newTriggerTime)

        AlarmModule.sendTaskSnoozedEvent(taskId, newTriggerTime)
    }

    private fun dismissAlarm() {
        val serviceIntent = Intent(this, AlarmService::class.java)
        stopService(serviceIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent dismissing with back button while alarm is active.
    }

    companion object {
        private const val TAG = "AlarmActivity"
    }
}
