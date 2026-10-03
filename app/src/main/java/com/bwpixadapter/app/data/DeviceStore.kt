package com.bwpixadapter.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class DeviceStore(context: Context) {
    private val prefs = context.getSharedPreferences("devices", Context.MODE_PRIVATE)
    private val _devices = MutableStateFlow(load())
    val devices: StateFlow<List<CameraDevice>> = _devices.asStateFlow()

    private fun load(): List<CameraDevice> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { CameraDevice.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun upsert(device: CameraDevice) {
        val list = _devices.value.toMutableList()
        val i = list.indexOfFirst { it.id == device.id }
        if (i >= 0) list[i] = device else list.add(device)
        save(list)
    }

    fun remove(id: String) {
        save(_devices.value.filterNot { it.id == id })
    }

    /** Remember the last entered credentials as defaults for the next device. */
    fun lastCredentials(): Pair<String, String> =
        (prefs.getString(KEY_USER, "") ?: "") to (prefs.getString(KEY_PWD, "") ?: "")

    fun saveCredentials(user: String, pwd: String) {
        if (user.isNotBlank()) {
            prefs.edit().putString(KEY_USER, user).putString(KEY_PWD, pwd).apply()
        }
    }

    private fun save(list: List<CameraDevice>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _devices.value = list
    }

    private companion object {
        const val KEY = "devices"
        const val KEY_USER = "last_user"
        const val KEY_PWD = "last_pwd"
    }
}
