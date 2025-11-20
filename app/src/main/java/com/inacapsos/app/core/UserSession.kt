package com.inacapsos.app.core

import android.content.Context
import androidx.preference.PreferenceManager

object UserSession {
    var userId: String? = null
        private set

    fun loadUserId(context: Context) {
        if (userId == null) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            userId = sharedPreferences.getString("user_id", null)
        }
    }

    fun saveUserId(context: Context, userId: String) {
        this.userId = userId
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putString("user_id", userId).apply()
    }

    fun clearUserId(context: Context) {
        this.userId = null
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().remove("user_id").apply()
    }
}
