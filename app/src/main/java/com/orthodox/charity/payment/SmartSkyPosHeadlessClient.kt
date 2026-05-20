package com.orthodox.charity.payment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.skytech.smartskyposlib.ISmartSkyPos
import com.skytech.smartskyposlib.State
import com.skytech.smartskyposlib.StateCallback
import com.skytech.smartskyposlib.TransactionCallback
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SmartSkyPosHeadlessException(message: String, cause: Throwable? = null) : Exception(message, cause)

class SmartSkyPosHeadlessClient(context: Context) {
    private val appContext = context.applicationContext

    @Volatile
    private var smartSkyPos: ISmartSkyPos? = null
    private var serviceConnection: ServiceConnection? = null
    private var bound = false
    private var stateCallbackRegistered = false

    suspend fun payment(
        params: TransactionParams,
        onStateChanged: (stateCode: Int, message: String?) -> Unit,
        onOperationNameChanged: (String?) -> Unit = {},
        onQrReading: (String?) -> Unit = {}
    ): TransactionResult {
        val pos = ensureConnected(onStateChanged)
        waitUntilReady(pos)

        val transactionCallback = object : TransactionCallback.Stub() {
            override fun onStateChanged(state: Int, message: String?) {
                onStateChanged(state, message)
            }

            override fun onQrReading(qr: String?) {
                onQrReading(qr)
            }

            override fun onOperationNameChanged(name: String?) {
                onOperationNameChanged(name)
            }

            override fun onRequestPassword(message: String?): String = ""
        }

        return withContext(Dispatchers.IO) {
            try {
                pos.payment(params, transactionCallback)
            } catch (t: Throwable) {
                throw SmartSkyPosHeadlessException("SmartSkyPos headless payment call failed", t)
            }
        }
    }

    fun cancelCardReading() {
        try {
            smartSkyPos?.cancelCardReading()
        } catch (_: Throwable) {
            // no-op: cancellation is best-effort
        }
    }

    fun close() {
        val pos = smartSkyPos
        val connection = serviceConnection

        if (pos != null && stateCallbackRegistered) {
            try {
                pos.unregisterStateCallback(stateCallback)
            } catch (_: Throwable) {
                // no-op
            }
        }
        stateCallbackRegistered = false

        if (bound && connection != null) {
            try {
                appContext.unbindService(connection)
            } catch (_: Throwable) {
                // no-op
            }
        }
        bound = false
        serviceConnection = null
        smartSkyPos = null
    }

    private suspend fun ensureConnected(
        onStateChanged: (stateCode: Int, message: String?) -> Unit
    ): ISmartSkyPos {
        smartSkyPos?.let { return it }

        return suspendCancellableCoroutine { cont ->
            val intent = Intent("com.skytech.smartskypos.ISmartSkyPos").apply {
                setPackage("com.skytech.smartskypos")
            }

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val pos = ISmartSkyPos.Stub.asInterface(service)
                    smartSkyPos = pos
                    if (pos != null) {
                        try {
                            pos.registerStateCallback(stateCallback)
                            stateCallbackRegistered = true
                        } catch (t: Throwable) {
                            if (cont.isActive) {
                                cont.resumeWithException(
                                    SmartSkyPosHeadlessException("Unable to register SmartSkyPos state callback", t)
                                )
                            }
                            return
                        }
                        stateEventHandler = onStateChanged
                        if (cont.isActive) cont.resume(pos)
                    } else if (cont.isActive) {
                        cont.resumeWithException(
                            SmartSkyPosHeadlessException("SmartSkyPos service connected with null binder")
                        )
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    smartSkyPos = null
                }
            }

            serviceConnection = connection
            val bindOk = appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bindOk) {
                serviceConnection = null
                cont.resumeWithException(SmartSkyPosHeadlessException("SmartSkyPos service bind failed"))
                return@suspendCancellableCoroutine
            }
            bound = true

            cont.invokeOnCancellation {
                close()
            }
        }
    }

    private suspend fun waitUntilReady(pos: ISmartSkyPos) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (true) {
            val state = try {
                pos.getState()
            } catch (t: Throwable) {
                throw SmartSkyPosHeadlessException("Failed to read SmartSkyPos state", t)
            }

            if (state == State.READY.code || state == State.UNFINISHED_OPERATION.code) {
                return@withContext
            }
            if (System.currentTimeMillis() - startTime >= 30_000L) {
                throw SmartSkyPosHeadlessException("SmartSkyPos state wait timeout")
            }
            delay(50L)
        }
    }

    @Volatile
    private var stateEventHandler: (stateCode: Int, message: String?) -> Unit = { _, _ -> }

    private val stateCallback = object : StateCallback.Stub() {
        override fun onStateChanged(state: Int, message: String?) {
            stateEventHandler(state, message)
        }
    }
}
