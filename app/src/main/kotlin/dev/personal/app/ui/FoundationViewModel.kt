package dev.personal.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.personal.app.data.DeviceRepository
import dev.personal.app.data.userMessage
import dev.personal.contracts.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FoundationState(
    val signedIn: Boolean = false,
    val busy: Boolean = false,
    val devices: List<Device> = emptyList(),
    val pairing: PairingCode? = null,
    val message: String? = null,
)

class FoundationViewModel(
    private val repository: DeviceRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutable = MutableStateFlow(FoundationState(signedIn = repository.hasSession()))
    val state = mutable.asStateFlow()
    init { if (mutable.value.signedIn) refresh() }
    fun signIn(credentials: Credentials, register: Boolean) = action {
        repository.signIn(credentials, register)
        mutable.value = mutable.value.copy(signedIn = true)
        load()
    }
    fun refresh() = action { load() }
    fun enroll(input: DeviceInput, code: String) = action {
        repository.enroll(input, code)
        load()
        mutable.value = mutable.value.copy(message = "Device registered.")
    }
    fun synchronize(id: String, input: DeviceInput) = action {
        repository.synchronize(id, input); load()
        mutable.value = mutable.value.copy(message = "Device status synchronized.")
    }
    fun pairing(installationId: String) = action { mutable.value = mutable.value.copy(pairing = repository.pairing(installationId)) }
    fun clearPairing() { mutable.value = mutable.value.copy(pairing = null) }
    fun revoke(id: String) = action {
        repository.revoke(id)
        load()
        mutable.value = mutable.value.copy(message = "Device access revoked.")
    }
    fun logout() = action {
        try { repository.logout() } finally { mutable.value = FoundationState() }
    }
    fun deleteAccount() = action { repository.deleteAccount(); mutable.value = FoundationState(message = "Account and server records deleted.") }
    private suspend fun load() { mutable.value = mutable.value.copy(devices = repository.devices()) }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, message = null)
        viewModelScope.launch(dispatcher) {
            try { block() }
            catch (failure: CancellationException) { throw failure }
            catch (failure: Exception) {
                val signedIn = repository.hasSession()
                mutable.value = if (signedIn) mutable.value.copy(message = userMessage(failure))
                    else FoundationState(message = userMessage(failure))
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
}