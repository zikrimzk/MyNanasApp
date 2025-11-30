package com.spm.mynanasapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.spm.mynanasapp.data.model.entity.User

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_PASSWORD = "user_password"
    private const val KEY_IS_REMEMBERED = "is_remembered"
    private const val KEY_USER_INFO = "user_info"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveAuthToken(context: Context, token: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }

    // === NEW: Function to Save the User Object ===
    fun saveUser(context: Context, user: User) {
        val editor = getPreferences(context).edit()
        val gson = Gson()
        // Convert the User object into a JSON String
        val json = gson.toJson(user)
        editor.putString(KEY_USER_INFO, json)
        editor.apply()
    }

    // === NEW: Function to Get the User Object ===
    fun getUser(context: Context): User? {
        val json = getPreferences(context).getString(KEY_USER_INFO, null)
        return if (json != null) {
            val gson = Gson()
            // Convert JSON String back to User object
            gson.fromJson(json, User::class.java)
        } else {
            null
        }
    }

    fun saveLoginCredentials(context: Context, username: String, pass: String) {
        val editor = getPreferences(context).edit()
        editor.putString(KEY_USER_NAME, username)
        editor.putString(KEY_PASSWORD, pass) // Note: Not encrypted (see security note below)
        editor.putBoolean(KEY_IS_REMEMBERED, true)
        editor.apply()
    }

    fun clearLoginCredentials(context: Context) {
        val editor = getPreferences(context).edit()
        editor.remove(KEY_USER_NAME)
        editor.remove(KEY_PASSWORD)
        editor.putBoolean(KEY_IS_REMEMBERED, false)
        editor.apply()
    }

    fun isRemembered(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_REMEMBERED, false)
    }

    fun getSavedUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_NAME, "")
    }

    fun getSavedPassword(context: Context): String? {
        return getPreferences(context).getString(KEY_PASSWORD, "")
    }

    fun clearSession(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }
}