package com.sendlater.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendlater.data.MessageRepository
import com.sendlater.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: MessageRepository,
    settings: SettingsStore
) : ViewModel() {
    val messages = repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val maker = settings.maker
    val mode = settings.mode
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}
