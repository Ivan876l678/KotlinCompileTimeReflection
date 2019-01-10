package ru.lozenko.kotlincompiletimereflection

import android.content.Context

/**
 * Created by Ivan Lozenko
 *
 *
 */

class ManualSettingsRealization(context: Context){

    private val sharedPreferences = context.getSharedPreferences("MANUAL_SETTINGS", Context.MODE_PRIVATE)

    var setting1: Boolean
    get() = sharedPreferences.getBoolean("setting1", false)
    set(value) = sharedPreferences.edit().putBoolean("setting1", value).apply()

    var setting2: String?
    get() = sharedPreferences.getString("setting2", null)
    set(value) = sharedPreferences.edit().putString("setting2", value).apply()

}