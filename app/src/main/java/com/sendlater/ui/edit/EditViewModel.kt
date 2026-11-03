package com.sendlater.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sendlater.data.MessageRepository
import com.sendlater.data.ScheduledMessage
import com.sendlater.data.SettingsStore
import com.sendlater.data.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val repo: MessageRepository,
    private val settings: SettingsStore
) : ViewModel() {
    val countryCode: String get() = settings.countryCode.value

    suspend fun load(id: Long) = repo.get(id)

    fun save(m: ScheduledMessage, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.save(m.copy(status = Status.PENDING, note = ""))
            onDone()
        }
    }
}
