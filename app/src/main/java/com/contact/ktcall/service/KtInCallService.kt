package com.contact.ktcall.service

import android.content.Intent
import android.os.IBinder
import com.contact.ktcall.di.CallModule

class KtInCallService : CallService() {

    override fun onCreate() {
        super.onCreate()
        CallModule.setCallService(this)
        // 启动前台通话管理服务
        CallForegroundService.startService(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止前台通话管理服务
        CallForegroundService.stopService(this)
    }
}