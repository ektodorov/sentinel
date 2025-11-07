package com.blogspot.techzealous.sentinel

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


class CameraService2: Service() {

    companion object {
        const val TAG = "CameraService2"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "camera_service_channel"
    }

    // indicates how to behave if the service is killed
    private var startMode: Int = Service.START_NOT_STICKY
    // interface for clients that bind
    private var binder: CameraService2Binder? = null
    // indicates whether onRebind should be used
    private var allowRebind: Boolean = false
    private var mExecutor: ExecutorService? = null
    private var isRunning: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate() {
        Log.i(TAG, "${Thread.currentThread().stackTrace[2].lineNumber}, CameraService2, onCreate")
        binder = CameraService2Binder()
        binder?.cameraService = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is starting, due to a call to startService()
        Log.i(TAG, "${Thread.currentThread().stackTrace[2].lineNumber}, CameraService2, onStartCommand")
        startForeground()
        myTask()
        return startMode
    }

    override fun onBind(intent: Intent): IBinder? {
        // A client is binding to the service with bindService()
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // All clients have unbound with unbindService()
        return allowRebind
    }

    override fun onRebind(intent: Intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    override fun onDestroy() {
        // The service is no longer used and is being destroyed
        Log.i(TAG, "${Thread.currentThread().stackTrace[2].lineNumber}, CameraService2, onDestroy")
    }

    private fun startForeground() {
        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = "SentinelChannel"
            val description = "Running foreground service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channelId = CHANNEL_ID

            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description

            val notificationManager = getSystemService<NotificationManager?>(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)

            notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(com.blogspot.techzealous.sentinel.R.mipmap.ic_launcher)
                .setContentTitle("Sentinel")
                .setContentText("Watching")
                .build()
        } else {
            notification = NotificationCompat.Builder(this)
                .setSmallIcon(com.blogspot.techzealous.sentinel.R.mipmap.ic_launcher)
                .setContentTitle("Sentinel")
                .setContentText("Watching")
                .build()
        }
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun myTask() {
        mExecutor = Executors.newSingleThreadExecutor()
        isRunning.getAndSet(true)
        val startTime = System.currentTimeMillis()
        mExecutor?.execute {
            while(isRunning.get()) {
                if((System.currentTimeMillis() - startTime) > (1 * 60 * 1000)) {
                    break
                }
                Thread.sleep(20000)
            }
            isRunning.getAndSet(false)
            stopSelf()
        }
    }
}