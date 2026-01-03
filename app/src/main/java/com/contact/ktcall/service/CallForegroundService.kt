package com.contact.ktcall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.contact.ktcall.R
import com.contact.ktcall.core.data.CallsState
import com.contact.ktcall.core.repository.CallRepository
import com.contact.ktcall.di.CallModule
import com.contact.ktcall.ui.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CallForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var callRepository: CallRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val CHANNEL_ID = "call_foreground_service"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        callRepository = CallModule.provideCallRepository()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createInitialNotification())

        // 监听通话状态变化，更新通知
        serviceScope.launch {
            callRepository.callsState.collectLatest { callsState ->
                updateNotification(callsState)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "通话管理服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "管理通话状态和快速进入通话界面"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createInitialNotification(): Notification {
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KtCall 通话管理")
            .setContentText("无活跃通话")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(callsState: CallsState) {
        val intent = Intent(this, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content) = when (callsState) {
            CallsState.NO_CALL -> "KtCall 通话管理" to "无活跃通话"
            CallsState.INCOMING -> "来电提醒" to "有新的来电"
            CallsState.OUTGOING -> "正在呼叫" to "拨号中..."
            CallsState.ACTIVE -> {
                serviceScope.launch {
                    callRepository.mainCall.collectLatest { call ->
                        val displayName = call?.displayName ?: call?.number ?: "未知号码"
                        val notification = NotificationCompat.Builder(this@CallForegroundService, CHANNEL_ID)
                            .setContentTitle("通话中")
                            .setContentText(displayName)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .build()
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }
                "通话中" to "正在通话..."
            }
            else -> "KtCall 通话管理" to "通话状态未知"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
