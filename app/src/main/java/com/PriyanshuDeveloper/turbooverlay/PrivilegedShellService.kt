package com.PriyanshuDeveloper.turbooverlay

import java.io.File
import kotlin.system.exitProcess

class PrivilegedShellService : IPrivilegedShellService.Stub() {
    override fun exec(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun readSystemFile(path: String): String {
        return try {
            File(path).readText()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}
