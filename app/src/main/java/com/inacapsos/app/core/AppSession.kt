package com.inacapsos.app.core

import android.content.Context
import android.content.SharedPreferences

object AppSession {
    private const val PREFS_NAME = "InacapSOS_prefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_ID_KEY = "user_id"
    private const val USER_NAME_KEY = "user_name"
    private const val USER_EMAIL_KEY = "user_email"
    private const val USER_ROLE_KEY = "user_role"

    private var prefs: SharedPreferences? = null

    var token: String? = null
    var userId: String? = null
    var userName: String? = null
    var userEmail: String? = null
    var userRole: String? = null // "GUARD" o "STUDENT" o 'ADMIN'

    fun create(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        token = prefs?.getString(TOKEN_KEY, null)
        userId = prefs?.getString(USER_ID_KEY, null)
        userName = prefs?.getString(USER_NAME_KEY, null)
        userEmail = prefs?.getString(USER_EMAIL_KEY, null)
        userRole = prefs?.getString(USER_ROLE_KEY, null)
    }

    fun save() {
        prefs?.edit()?.apply {
            putString(TOKEN_KEY, token)
            putString(USER_ID_KEY, userId)
            putString(USER_NAME_KEY, userName)
            putString(USER_EMAIL_KEY, userEmail)
            putString(USER_ROLE_KEY, userRole)
            apply()
        }
    }

    /**
     * Limpia todos los datos de la sesión actual.
     */
    fun clear() {
        token = null
        userId = null
        userName = null
        userEmail = null
        userRole = null
        prefs?.edit()?.clear()?.apply()
    }
}
