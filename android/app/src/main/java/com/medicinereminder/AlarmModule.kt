package com.medicinereminder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.content.Context
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class AlarmModule(private val reactContext: ReactApplicationContext) : 
    ReactContextBaseJavaModule(reactContext) {
    
    override fun getName() = "AlarmModule"
    
    companion object {
        private var instance: AlarmModule? = null
        
        fun sendTaskCompletedEvent(taskId: String) {
            instance?.sendEvent("onTaskCompleted", Arguments.createMap().apply {
                putString("taskId", taskId)
            })
        }
        
        fun sendTaskSnoozedEvent(taskId: String, newTime: Long) {
            instance?.sendEvent("onTaskSnoozed", Arguments.createMap().apply {
                putString("taskId", taskId)
                putDouble("newTime", newTime.toDouble())
            })
        }
    }
    
    init {
        instance = this
    }
    
    @ReactMethod
    fun scheduleAlarm(taskId: String, taskName: String, triggerTimeMillis: Double, promise: Promise) {
        try {
            val scheduler = AlarmScheduler(reactContext)
            val success = scheduler.scheduleAlarm(taskId, taskName, triggerTimeMillis.toLong())
            promise.resolve(success)
        } catch (e: Exception) {
            promise.reject("SCHEDULE_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun cancelAlarm(taskId: String, promise: Promise) {
        try {
            val scheduler = AlarmScheduler(reactContext)
            scheduler.cancelAlarm(taskId)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun requestExactAlarmPermission(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                reactContext.startActivity(intent)
                promise.resolve(true)
            } else {
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("PERMISSION_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun requestBatteryOptimization(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${reactContext.packageName}")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                reactContext.startActivity(intent)
                promise.resolve(true)
            } else {
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("BATTERY_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun requestDisplayOverApps(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(reactContext)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${reactContext.packageName}")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    reactContext.startActivity(intent)
                }
                promise.resolve(true)
            } else {
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("DISPLAY_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun checkPermissions(promise: Promise) {
        try {
            val result = Arguments.createMap()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = reactContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                result.putBoolean("exactAlarm", alarmManager.canScheduleExactAlarms())
            } else {
                result.putBoolean("exactAlarm", true)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = reactContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                result.putBoolean("batteryOptimization", 
                    powerManager.isIgnoringBatteryOptimizations(reactContext.packageName))
            } else {
                result.putBoolean("batteryOptimization", true)
            }
            
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("CHECK_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun checkDisplayOverApps(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                promise.resolve(Settings.canDrawOverlays(reactContext))
            } else {
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("CHECK_ERROR", e.message, e)
        }
    }
    
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}