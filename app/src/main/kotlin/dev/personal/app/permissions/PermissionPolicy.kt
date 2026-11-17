package dev.personal.app.permissions

enum class PermissionAccess { GRANTED, REQUESTABLE, RATIONALE, SETTINGS }

fun permissionAccess(granted: Boolean, requestedBefore: Boolean, showRationale: Boolean): PermissionAccess = when {
    granted -> PermissionAccess.GRANTED
    showRationale -> PermissionAccess.RATIONALE
    requestedBefore -> PermissionAccess.SETTINGS
    else -> PermissionAccess.REQUESTABLE
}