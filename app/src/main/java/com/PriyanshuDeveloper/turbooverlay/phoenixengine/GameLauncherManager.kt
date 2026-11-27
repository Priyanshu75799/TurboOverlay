package com.PriyanshuDeveloper.turbooverlay.phoenixengine

import android.content.Context
import android.content.SharedPreferences

object GameLauncherManager {

    private const val PREF_NAME = "turbo_overlay_prefs"
    private const val KEY_GAME_PACKAGES = "key_game_packages"
    private const val KEY_LAG_PROTECTION = "key_lag_protection"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedGamePackages(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_GAME_PACKAGES, emptySet()) ?: emptySet()
    }

    fun saveGamePackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_GAME_PACKAGES, HashSet(packages)).apply()
    }

    fun removeGamePackage(context: Context, packageName: String) {
        val current = getSavedGamePackages(context).toMutableSet()
        current.remove(packageName)
        saveGamePackages(context, current)
    }

    fun isLagProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LAG_PROTECTION, false)
    }

    fun setLagProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LAG_PROTECTION, enabled).apply()
    }
}
