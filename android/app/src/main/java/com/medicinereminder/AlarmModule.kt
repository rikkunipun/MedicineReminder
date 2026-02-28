package com.medicinereminder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.Calendar

class AlarmModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "AlarmModule"

    companion object {
        private const val TAG = "AlarmModule"
        const val PREFS_NAME = "AlarmTaskPrefs"
        private var instance: AlarmModule? = null

        fun sendTaskCompletedEvent(taskId: String) {
            instance?.sendEvent("onTaskCompleted", Arguments.createMap().apply {
                putString("taskId", taskId)
            })
        }

        fun sendTaskRescheduledEvent(taskId: String, newScheduledTimeMillis: Long) {
            Log.d(TAG, "Sending onTaskRescheduled event: taskId=$taskId, newTime=$newScheduledTimeMillis")
            instance?.sendEvent("onTaskRescheduled", Arguments.createMap().apply {
                putString("taskId", taskId)
                putDouble("newScheduledTime", newScheduledTimeMillis.toDouble())
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

    // Saves recurring task info to SharedPreferences so AlarmActivity can
    // access it reliably without depending on AsyncStorage's SQLite.
    private fun saveTaskInfoToPrefs(taskId: String, taskName: String, isRecurring: Boolean, triggerTimeMillis: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = triggerTimeMillis
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("task_name_$taskId", taskName)
            .putBoolean("task_recurring_$taskId", isRecurring)
            .putInt("task_hour_$taskId", hour)
            .putInt("task_minute_$taskId", minute)
            .apply()

        Log.d(TAG, "Saved task info to prefs: id=$taskId, name=$taskName, recurring=$isRecurring, hour=$hour, minute=$minute")
    }

    @ReactMethod
    fun scheduleAlarm(taskId: String, taskName: String, triggerTimeMillis: Double, isRecurring: Boolean, promise: Promise) {
        try {
            val triggerMillis = triggerTimeMillis.toLong()
            val scheduler = AlarmScheduler(reactContext)
            val success = scheduler.scheduleAlarm(taskId, taskName, triggerMillis)
            if (success) {
                saveTaskInfoToPrefs(taskId, taskName, isRecurring, triggerMillis)
            }
            promise.resolve(success)
        } catch (e: Exception) {
            Log.e(TAG, "scheduleAlarm error", e)
            promise.reject("SCHEDULE_ERROR", e.message, e)
        }
    }
    
    @ReactMethod
    fun cancelAlarm(taskId: String, promise: Promise) {
        try {
            val scheduler = AlarmScheduler(reactContext)
            scheduler.cancelAlarm(taskId)
            // Clean up SharedPreferences entry for this task.
            reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove("task_name_$taskId")
                .remove("task_recurring_$taskId")
                .remove("task_hour_$taskId")
                .remove("task_minute_$taskId")
                .apply()
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
    
    // Required by NativeEventEmitter on the JS side.
    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}