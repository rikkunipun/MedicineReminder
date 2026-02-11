package com.medicinereminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleAlarm(taskId: String, taskName: String, triggerTimeMillis: Long): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("AlarmScheduler", "No exact alarm permission")
                    return false
                }
            }
            
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.medicinereminder.ALARM_TRIGGER"
                putExtra("TASK_ID", taskId)
                putExtra("TASK_NAME", taskName)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmClockInfo = AlarmManager.AlarmClockInfo(
                triggerTimeMillis,
                pendingIntent
            )
            
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            
            Log.d("AlarmScheduler", "Alarm scheduled: $taskName at $triggerTimeMillis")
            return true
            
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error scheduling alarm", e)
            return false
        }
    }
    
    fun cancelAlarm(taskId: String) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.medicinereminder.ALARM_TRIGGER"
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmScheduler", "Alarm cancelled: $taskId")
            
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error cancelling alarm", e)
        }
    }
}