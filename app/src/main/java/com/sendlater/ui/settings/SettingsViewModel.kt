package com.sendlater.ui.settings

import androidx.lifecycle.ViewModel
import com.sendlater.data.SendMode
import com.sendlater.data.SettingsStore
import com.sendlater.data.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val mode = store.mode
    val theme = store.theme
    val maker = store.maker
    val countryCode = store.countryCode
    fun setMode(m: SendMode) = store.setMode(m)
    fun setTheme(t: ThemeMode) = store.setTheme(t)
    fun setMaker(n: String) = store.setMaker(n)
    fun setCountryCode(c: String) = store.setCountryCode(c)
}
