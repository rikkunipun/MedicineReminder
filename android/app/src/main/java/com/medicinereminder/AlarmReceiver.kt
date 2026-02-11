package com.medicinereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm triggered!")
        
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskName = intent.getStringExtra("TASK_NAME") ?: return
        
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_NAME", taskName)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
