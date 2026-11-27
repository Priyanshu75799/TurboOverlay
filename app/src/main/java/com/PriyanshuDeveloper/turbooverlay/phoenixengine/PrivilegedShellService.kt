package com.PriyanshuDeveloper.turbooverlay.phoenixengine

import android.os.IBinder
import com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

class PrivilegedShellService : IPrivilegedShellService.Stub() {

    override fun exec(command: String?): String {
        if (command.isNullOrBlank()) return ""
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    override fun readSystemFile(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim()
            } else {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat $path"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }

                process.waitFor()
                output.toString().trim()
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
