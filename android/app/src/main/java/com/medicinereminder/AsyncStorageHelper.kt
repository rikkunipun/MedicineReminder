package com.medicinereminder

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log

object AsyncStorageHelper {
    private const val TAG = "AsyncStorageHelper"
    private const val DATABASE_NAME = "RKStorage"
    private const val TABLE_NAME = "catalystLocalStorage"
    private const val COLUMN_KEY = "key"
    private const val COLUMN_VALUE = "value"
    private const val TASKS_KEY = "@medicine_tasks"

    private fun getDatabase(context: Context): SQLiteDatabase? {
        return try {
            val dbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
            SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening AsyncStorage database", e)
            null
        }
    }

    fun getTasksJson(context: Context): String {
        val db = getDatabase(context) ?: return "[]"
        return try {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COLUMN_VALUE),
                "$COLUMN_KEY = ?",
                arrayOf(TASKS_KEY),
                null, null, null
            )
            val result = if (cursor.moveToFirst()) {
                cursor.getString(0) ?: "[]"
            } else {
                "[]"
            }
            cursor.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from AsyncStorage", e)
            "[]"
        } finally {
            db.close()
        }
    }

    fun saveTasksJson(context: Context, tasksJson: String): Boolean {
        val db = getDatabase(context) ?: return false
        return try {
            val values = android.content.ContentValues().apply {
                put(COLUMN_KEY, TASKS_KEY)
                put(COLUMN_VALUE, tasksJson)
            }
            val rowsAffected = db.update(
                TABLE_NAME,
                values,
                "$COLUMN_KEY = ?",
                arrayOf(TASKS_KEY)
            )
            if (rowsAffected == 0) {
                db.insert(TABLE_NAME, null, values)
            }
            Log.d(TAG, "Tasks saved to AsyncStorage successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tasks to AsyncStorage", e)
            false
        } finally {
            db.close()
        }
    }
}
