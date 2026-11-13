package com.example.data.device

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SystemRoleStatus(
    val isDefaultDialer: Boolean,
    val isCallScreeningApp: Boolean,
    val canRequestDialerRole: Boolean,
    val canRequestCallScreeningRole: Boolean
)

object SystemRoleHelper {

    fun getRoleStatus(context: Context): SystemRoleStatus {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val isDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true ||
                telecomManager?.defaultDialerPackage == context.packageName
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }

        val isScreening = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        val canDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleAvailable(RoleManager.ROLE_DIALER) == true
        } else {
            true
        }

        val canScreening = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
                ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }

        return SystemRoleStatus(
            isDefaultDialer = isDialer,
            isCallScreeningApp = isScreening,
            canRequestDialerRole = canDialer,
            canRequestCallScreeningRole = canScreening
        )
    }

    fun createDefaultDialerIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else null
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(
                    TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                    context.packageName
                )
            }
        }
    }

    fun createCallScreeningIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } else null
        } else {
            null
        }
    }

    fun placePhoneCall(context: Context, rawNumber: String) {
        val cleaned = rawNumber.trim()
        if (cleaned.isBlank()) return
        val telUri = Uri.fromParts("tel", cleaned, null)
        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

        if (isDefaultDialer && hasCallPerm && telecomManager != null) {
            try {
                telecomManager.placeCall(telUri, Bundle())
                return
            } catch (_: SecurityException) {
            }
        }

        val action = if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val intent = Intent(action, telUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
