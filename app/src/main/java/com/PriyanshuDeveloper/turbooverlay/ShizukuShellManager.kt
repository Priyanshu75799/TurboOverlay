package com.PriyanshuDeveloper.turbooverlay

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs

object ShizukuShellManager {
    @Volatile
    private var service: IPrivilegedShellService? = null
    @Volatile
    private var isBinding = false
    private val handler = Handler(Looper.getMainLooper())
    private val bindTimeoutRunnable = Runnable {
        if (isBinding && service == null) {
            isBinding = false
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IPrivilegedShellService.Stub.asInterface(binder)
            isBinding = false
            handler.removeCallbacks(bindTimeoutRunnable)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBinding = false
            handler.removeCallbacks(bindTimeoutRunnable)
        }

        override fun onBindingDied(name: ComponentName?) {
            service = null
            isBinding = false
            handler.removeCallbacks(bindTimeoutRunnable)
        }

        override fun onNullBinding(name: ComponentName?) {
            service = null
            isBinding = false
            handler.removeCallbacks(bindTimeoutRunnable)
        }
    }

    private var hasListener = false

    fun isReady(): Boolean = service != null

    fun init(context: Context) {
        if (!hasListener) {
            try {
                Shizuku.addBinderReceivedListener { init(context.applicationContext) }
                hasListener = true
            } catch (_: Exception) {}
        }
        if (isReady() || isBinding) return
        if (!Shizuku.pingBinder()) return

        try {
            val args = UserServiceArgs(ComponentName(context.packageName, PrivilegedShellService::class.java.name))
                .daemon(false)
                .processNameSuffix("privileged_shell")
                .debuggable(BuildConfig.DEBUG)
                .version(1)

            Shizuku.bindUserService(args, connection)
            isBinding = true
            handler.postDelayed(bindTimeoutRunnable, 5000)
        } catch (_: Exception) {
            isBinding = false
            handler.removeCallbacks(bindTimeoutRunnable)
        }
    }

    fun exec(command: String): String? {
        return try {
            service?.exec(command)
        } catch (_: Exception) {
            null
        }
    }

    fun readFile(path: String): String? {
        return try {
            service?.readSystemFile(path)
        } catch (_: Exception) {
            null
        }
    }

    fun destroy() {
        try {
            service?.destroy()
        } catch (_: Exception) {}
        service = null
        isBinding = false
        handler.removeCallbacks(bindTimeoutRunnable)
    }
}
