package com.inacapsos.app.core

object AppSession {
    var token: String? = null
    var userId: String? = null
    var userName: String? = null
    var userEmail: String? = null
    var userRole: String? = null // "GUARD" o "STUDENT" o 'ADMIN'

    /**
     * Limpia todos los datos de la sesión actual.
     */
    fun clear() {
        token = null
        userId = null
        userName = null
        userEmail = null
        userRole = null
    }
}
