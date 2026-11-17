package dev.personal.app.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionPolicyTest {
    @Test fun currentGrantOverridesAllHistory() {
        listOf(false, true).forEach { requested ->
            listOf(false, true).forEach { rationale ->
                assertEquals(PermissionAccess.GRANTED, permissionAccess(true, requested, rationale))
            }
        }
    }
    @Test fun firstRequestDoesNotAssumePermanentDenial() {
        assertEquals(PermissionAccess.REQUESTABLE, permissionAccess(false, false, false))
    }
    @Test fun denialWithRationaleCanBeRequestedAgain() {
        assertEquals(PermissionAccess.RATIONALE, permissionAccess(false, true, true))
    }
    @Test fun platformRationaleWinsWhenLocalHistoryIsMissing() {
        assertEquals(PermissionAccess.RATIONALE, permissionAccess(false, false, true))
    }
    @Test fun priorRequestWithoutRationaleOffersSettings() {
        assertEquals(PermissionAccess.SETTINGS, permissionAccess(false, true, false))
    }
}