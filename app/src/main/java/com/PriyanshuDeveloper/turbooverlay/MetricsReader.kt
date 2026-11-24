package com.PriyanshuDeveloper.turbooverlay

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Locale
import kotlin.math.abs

class MetricsReader {

    private var lastTotal: Long = 0
    private var lastIdle: Long = 0

    private var lastNetTime = 0L
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L

    class NetSpeedMetrics {
        var downKBps: Float = 0f
        var upKBps: Float = 0f
        var latencyMs: Int = -1
        var signalDbm: Int = 0
    }

    fun getSignalStrength(): Int {
        return try {
            val output = ShizukuShellManager.exec("dumpsys telephony.registry") ?: return 0
            val lines = output.split("\n").filter { it.contains("SignalStrength", ignoreCase = true) }

            val regex = Regex("(?i)(?:dbm|rsrp|rsrq|ssrsrp)=([-\\d]+)")

            for (line in lines) {
                val matches = regex.findAll(line)
                for (match in matches) {
                    val dbm = match.groupValues[1].toIntOrNull() ?: 0
                    if (dbm in -199..-31) return dbm
                }
            }

            val fallbackRegex = Regex("-(\\d{2,3})")
            for (line in lines) {
                val matches = fallbackRegex.findAll(line)
                for (match in matches) {
                    val valInt = match.value.toIntOrNull() ?: 0
                    if (valInt in -199..-31) return -valInt
                }
            }
            0
        } catch (_: Exception) { 0 }
    }

    fun getLatency(): Int {
        return try {
            val output = ShizukuShellManager.exec("ping -c 1 -w 1 8.8.8.8")
            if (!output.isNullOrBlank()) {
                val regex = Regex("""time[=<]\s*([\d.]+)\s*ms""", RegexOption.IGNORE_CASE)
                val match = regex.find(output)
                if (match != null) {
                    return match.groupValues[1].toFloatOrNull()?.toInt() ?: -1
                }
            }
            -1
        } catch (_: Exception) { -1 }
    }

    fun getNetSpeed(out: NetSpeedMetrics) {
        val now = System.currentTimeMillis()
        val output = ShizukuShellManager.readFile("/proc/net/dev") ?: return

        var rx = 0L
        var tx = 0L

        val lines = output.split("\n")
        for (line in lines) {
            if (line.contains(":") && !line.contains("lo:")) {
                val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (parts.size >= 10) {
                    rx += parseLong(parts[1])
                    tx += parseLong(parts[9])
                }
            }
        }

        if (lastNetTime != 0L) {
            val deltaTime = (now - lastNetTime) / 1000f
            if (deltaTime > 0f) {
                out.downKBps = ((rx - lastRxBytes) / 1024f / deltaTime).coerceAtLeast(0f)
                out.upKBps = ((tx - lastTxBytes) / 1024f / deltaTime).coerceAtLeast(0f)
            }
        }

        lastRxBytes = rx
        lastTxBytes = tx
        lastNetTime = now
    }

    fun getCpuUsage(): Float {
        return try {
            val output = ShizukuShellManager.readFile("/proc/stat") ?: return 0f
            var pos = output.indexOf("cpu ")
            if (pos == -1) return 0f

            pos += 4
            while (pos < output.length && output[pos] == ' ') pos++

            var total = 0L
            var idle = 0L
            var fieldIdx = 0

            while (pos < output.length && output[pos] != '\n') {
                val start = pos
                while (pos < output.length && output[pos] != ' ' && output[pos] != '\n') pos++

                if (pos > start) {
                    var value = 0L
                    for (i in start until pos) {
                        val digit = output[i] - '0'
                        if (digit in 0..9) value = value * 10 + digit
                    }
                    total += value
                    if (fieldIdx == 3 || fieldIdx == 4) idle += value
                    fieldIdx++
                }
                while (pos < output.length && output[pos] == ' ') pos++
            }

            if (lastTotal == 0L) {
                lastTotal = total
                lastIdle = idle
                return 0f
            }

            val diffTotal = total - lastTotal
            val diffIdle = idle - lastIdle
            lastTotal = total
            lastIdle = idle

            if (diffTotal <= 0L) 0f else (1f - diffIdle.toFloat() / diffTotal).coerceIn(0f, 1f) * 100f
        } catch (_: Exception) {
            0f
        }
    }

    private fun formatFreq(rawStr: String?): String? {
        if (rawStr.isNullOrBlank() || rawStr.startsWith("Error")) return null
        val rawVal = parseLong(rawStr)
        if (rawVal <= 0) return null

        val khz: Long = when {
            rawVal > 10_000_000L -> rawVal / 1000L
            rawVal in 100L..10_000L -> rawVal * 1000L
            else -> rawVal
        }

        return if (khz >= 1_000_000L) {
            String.format(Locale.US, "%.2fGHz", khz / 1_000_000f)
        } else {
            "${khz / 1000L}MHz"
        }
    }

    fun getCpuCoreFreqs(): Array<String> {
        val freqs = Array(8) { "Off" }
        var hasAnySuccess = false

        for (i in 0..7) {
            val onlineStr = ShizukuShellManager.readFile("/sys/devices/system/cpu/cpu$i/online")
            if (onlineStr != null && onlineStr.trim() == "0") {
                freqs[i] = "Off"
                continue
            }

            var fStr = ShizukuShellManager.readFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            var formatted = formatFreq(fStr)

            if (formatted == null) {
                fStr = ShizukuShellManager.readFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_cur_freq")
                formatted = formatFreq(fStr)
            }

            if (formatted != null) {
                freqs[i] = formatted
                hasAnySuccess = true
            } else {
                val existsStr = ShizukuShellManager.readFile("/sys/devices/system/cpu/cpu$i/uevent")
                if (existsStr != null && existsStr.startsWith("Error")) {
                    freqs[i] = "N/A"
                } else {
                    freqs[i] = "Off"
                }
            }
        }

        if (!hasAnySuccess) {
            val batchOutput = ShizukuShellManager.exec("cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq 2>/dev/null || cat /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_cur_freq 2>/dev/null")
            if (!batchOutput.isNullOrBlank() && !batchOutput.startsWith("Error")) {
                val lines = batchOutput.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                for (i in 0 until minOf(8, lines.size)) {
                    val formatted = formatFreq(lines[i])
                    if (formatted != null) {
                        freqs[i] = formatted
                    }
                }
            }
        }

        return freqs
    }

    class BatteryPowerMetrics {
        var voltageV: Float = 0f
        var currentMA: Float = 0f
        var powerW: Float = 0f
        var isCharging: Boolean = false
    }

    fun getBatteryPowerMetrics(context: Context, out: BatteryPowerMetrics) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        val statusApi = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        var isCharging = statusApi == BatteryManager.BATTERY_STATUS_CHARGING || statusApi == BatteryManager.BATTERY_STATUS_FULL

        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = try { context.registerReceiver(null, ifilter) } catch (_: Exception) { null }

        if (!isCharging) {
            val statusIntent = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = statusIntent == BatteryManager.BATTERY_STATUS_CHARGING || statusIntent == BatteryManager.BATTERY_STATUS_FULL
        }
        out.isCharging = isCharging

        var voltageV = 0f
        var currentMA = 0f

        val vStr = ShizukuShellManager.readFile("/sys/class/power_supply/battery/voltage_now")
        val cStr = ShizukuShellManager.readFile("/sys/class/power_supply/battery/current_now")

        if (!vStr.isNullOrBlank() && !vStr.startsWith("Error")) {
            val vRaw = parseLong(vStr)
            voltageV = abs(vRaw / 1_000_000f)
            if (voltageV < 1.0f) voltageV = abs(vRaw / 1000f)
        }
        if (!cStr.isNullOrBlank() && !cStr.startsWith("Error")) {
            val cRaw = parseLong(cStr)
            currentMA = cRaw / 1000f
            if (abs(currentMA) > 50000f) currentMA /= 1000f
        }

        if (voltageV < 2.0f || voltageV > 6.0f) {
            val vApi = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            if (vApi > 0) voltageV = vApi / 1000f
        }

        if (currentMA == 0f) {
            val cApi = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0L
            currentMA = cApi / 1000f
        }

        if (isCharging) {
            currentMA = abs(currentMA)
        } else {
            currentMA = -abs(currentMA)
        }

        out.voltageV = voltageV.coerceIn(0f, 6f)
        out.currentMA = currentMA
        out.powerW = out.voltageV * (out.currentMA / 1000f)
    }

    fun getBatteryTemp(context: Context): Float {
        val tStr = ShizukuShellManager.readFile("/sys/class/power_supply/battery/temp")
        if (!tStr.isNullOrBlank() && !tStr.startsWith("Error")) {
            val t = parseLong(tStr).toFloat()
            if (t != 0f) {
                return if (t > 200f) (t / 10f).coerceIn(10f, 75f)
                else t.coerceIn(10f, 75f)
            }
        }

        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = try { context.registerReceiver(null, ifilter) } catch (_: Exception) { null }
        val tempInt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        if (tempInt != 0) {
            return (tempInt.toFloat() / 10f).coerceIn(10f, 75f)
        }

        return 36.6f
    }

    private fun parseLong(s: String?): Long {
        if (s == null) return 0L
        var value = 0L
        var sign = 1
        for (char in s.trim()) {
            if (char == '-') sign = -1
            else {
                val digit = char - '0'
                if (digit in 0..9) value = value * 10 + digit
            }
        }
        return value * sign
    }

    fun executeShellCommand(command: String): String {
        return ShizukuShellManager.exec(command) ?: "Not Ready"
    }
}

fun Float.formatNetSpeed(): String {
    if (this.isNaN() || this.isInfinite()) return "0.0 KB/s"

    val bytesStr = when {
        this >= 1024f -> String.format(Locale.US, "%.2f MB/s", this / 1024f)
        else -> String.format(Locale.US, "%.1f KB/s", this)
    }

    val kbps = this * 8f
    val bitsStr = when {
        kbps >= 1000f -> String.format(Locale.US, "%.2f Mbps", kbps / 1000f)
        else -> String.format(Locale.US, "%.1f kbps", kbps)
    }

    return "$bytesStr ($bitsStr)"
}

fun Float.format3Decimals(): String {
    if (this.isNaN() || this.isInfinite()) return "N/A"
    return String.format(Locale.US, "%.3f", this)
}

fun Float.format1Decimal(): String {
    if (this.isNaN() || this.isInfinite()) return "N/A"
    return String.format(Locale.US, "%.1f", this)
}
