package com.example.myapplication.data.preferences

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object SessionManager {

    @Volatile var userId: Int? = null
    @Volatile var token: String? = null
    @Volatile var userName: String? = null
    @Volatile var userEmail: String? = null
    @Volatile var empCode: String? = null

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val prefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            token = prefs.userToken.first()
            userId = prefs.userId.first()
            userName = prefs.userName.first()
            userEmail = prefs.userEmail.first()
            empCode = prefs.userEmpCode.first()
        }
    }

    /**
     * Guarda la sesión en memoria Y en DataStore.
     */
    fun setSession(
        context: Context,
        userId: Int,
        token: String,
        name: String,
        email: String,
        empCode: String? = null
    ) {
        this.userId = userId
        this.token = token
        this.userName = name
        this.userEmail = email
        this.empCode = empCode

        val prefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            prefs.saveUser(
                name = name,
                token = token,
                id = userId,
                email = email,
                empCode = empCode
            )
        }
    }

    /**
     * Limpia TODO en memoria y en DataStore.
     */
    fun clear(context: Context) {
        this.userId = null
        this.token = null
        this.userName = null
        this.userEmail = null
        this.empCode = null

        val prefs = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            prefs.clearUser()
        }
    }
}
