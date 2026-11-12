package dev.personal.app.ui

import dev.personal.app.data.DeviceRepository
import dev.personal.contracts.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoundationViewModelTest {
    private class FakeRepository : DeviceRepository {
        var signedIn = false
        var failLoad = false
        var signIns = 0
        override fun hasSession() = signedIn
        override suspend fun signIn(credentials: Credentials, register: Boolean) { signIns++; signedIn = true }
        override suspend fun devices(): List<Device> { if (failLoad) throw java.io.IOException(); return emptyList() }
        override suspend fun enroll(input: DeviceInput, pairingCode: String): Device = error("Not used by this test")
        override suspend fun synchronize(id: String, input: DeviceInput): Device = error("Not used by this test")
        override suspend fun pairing(installationId: String) = PairingCode("test", 100)
        override suspend fun revoke(id: String) = Unit
        override suspend fun logout() { signedIn = false }
        override suspend fun deleteAccount() { signedIn = false }
    }
    @Test fun signInBusyGuardAndLogout() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository()
            val model = FoundationViewModel(repository, dispatcher)
            val credentials = Credentials("owner@example.com", "password")
            model.signIn(credentials, false)
            model.signIn(credentials, false)
            assertTrue(model.state.value.busy)
            advanceUntilIdle()
            assertEquals(1, repository.signIns)
            assertTrue(model.state.value.signedIn)
            assertFalse(model.state.value.busy)
            model.logout(); advanceUntilIdle()
            assertFalse(model.state.value.signedIn)
            assertTrue(model.state.value.devices.isEmpty())
        } finally { Dispatchers.resetMain() }
    }
    @Test fun networkFailureKeepsSessionAndExposesError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeRepository().apply { signedIn = true; failLoad = true }
            val model = FoundationViewModel(repository, dispatcher)
            advanceUntilIdle()
            assertTrue(model.state.value.signedIn)
            assertFalse(model.state.value.busy)
            assertNotNull(model.state.value.message)
        } finally { Dispatchers.resetMain() }
    }
}