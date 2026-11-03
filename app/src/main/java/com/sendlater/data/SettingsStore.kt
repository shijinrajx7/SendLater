package com.sendlater.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SendMode { SAFE, AUTO }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(SendMode.valueOf(prefs.getString("mode", "AUTO")!!))
    val mode: StateFlow<SendMode> = _mode

    private val _theme = MutableStateFlow(ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!))
    val theme: StateFlow<ThemeMode> = _theme

    private val _maker = MutableStateFlow(prefs.getString("maker", "Shijin") ?: "Shijin")
    val maker: StateFlow<String> = _maker

    fun setMaker(n: String) {
        prefs.edit().putString("maker", n).apply(); _maker.value = n
    }

    private val _cc = MutableStateFlow(prefs.getString("cc", "91") ?: "91")
    val countryCode: StateFlow<String> = _cc

    fun setCountryCode(c: String) {
        val d = c.filter { it.isDigit() }.take(4)
        prefs.edit().putString("cc", d).apply(); _cc.value = d
    }

    fun setMode(m: SendMode) {
        prefs.edit().putString("mode", m.name).apply(); _mode.value = m
    }

    fun setTheme(t: ThemeMode) {
        prefs.edit().putString("theme", t.name).apply(); _theme.value = t
    }
}
