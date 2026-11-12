package dev.personal.contracts

import org.junit.Assert.*
import org.junit.Test

class ContractsTest {
    private val valid = DeviceInput("00000000-0000-0000-0000-000000000001", "Phone", "Motorola", "Test", "13", 50, "wifi")
    @Test fun validatesBatteryIdentityAndConnectivity() {
        assertTrue(valid.isValid())
        assertFalse(valid.copy(batteryPercent = 101).isValid())
        assertFalse(valid.copy(installationId = "device").isValid())
        assertFalse(valid.copy(connectivity = "unknown").isValid())
        assertFalse(valid.copy(name = " ").isValid())
    }
}