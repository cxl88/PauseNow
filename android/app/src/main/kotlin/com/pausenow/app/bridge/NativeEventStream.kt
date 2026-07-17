package com.pausenow.app.bridge

import android.os.Handler
import android.os.Looper
import com.pausenow.app.accessibility.ForegroundPackageEventRecord
import io.flutter.plugin.common.EventChannel

object NativeEventStream : EventChannel.StreamHandler {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var eventSink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    fun publish(event: ForegroundPackageEventRecord) {
        mainHandler.post {
            eventSink?.success(event.toChannelMap())
        }
    }
}
