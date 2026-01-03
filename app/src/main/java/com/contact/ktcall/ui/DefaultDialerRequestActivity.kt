package com.contact.ktcall.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.contact.ktcall.core.permission.PermissionRepository
import com.contact.ktcall.core.permission.PermissionRepositoryImpl
import kotlinx.coroutines.launch

class DefaultDialerRequestActivity : ComponentActivity() {
    lateinit var permissionUseCase: PermissionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            checkDefaultDialer()
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DEFAULT_DIALER) {
            lifecycleScope.launch {
                when (resultCode) {
                    Activity.RESULT_OK -> permissionUseCase.entryDefaultDialerResult(true)
                    else -> permissionUseCase.entryDefaultDialerResult(false)
                }
            }
        }
        finish()
    }


    private fun checkDefaultDialer() {
        permissionUseCase = PermissionRepositoryImpl((getSystemService(Context.TELECOM_SERVICE) as TelecomManager), this)
        if (permissionUseCase.isDefaultDialer) {
            lifecycleScope.launch {
                permissionUseCase.entryDefaultDialerResult(true)
                finish()
            }
        }

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (getSystemService(Context.ROLE_SERVICE) as RoleManager).createRequestRoleIntent(
                RoleManager.ROLE_DIALER
            )
        } else {
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                this.packageName
            )
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_DEFAULT_DIALER)
    }

    companion object {
        const val REQUEST_CODE_DEFAULT_DIALER = 1
    }
}