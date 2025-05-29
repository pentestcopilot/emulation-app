package com.emulation

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import java.io.File

object EmulatorDetector {

    private const val TAG = "EmulatorDetector"
    private const val EMULATOR_SCORE_THRESHOLD = 1

    private val suspiciousBuildProperties = listOf(
        Property("FINGERPRINT", ::startsWithAny, listOf("generic")),
        Property("FINGERPRINT", ::containsAny, listOf("generic", "vbox", "test-keys", "android")),
        Property("MODEL", ::equalsAny, listOf("sdk", "google_sdk", "Android SDK built for x86", "Android SDK built for x86_64")),
        Property("MANUFACTURER", ::equalsAny, listOf("unknown", "Genymotion")),
        Property("HARDWARE", ::equalsAny, listOf("goldfish", "vbox86")),
        Property("PRODUCT", ::containsAny, listOf("sdk", "Andy", "google_sdk", "Droid4X", "nox")),
        Property("BRAND", ::equalsAny, listOf("generic", "generic_x86")),
        Property("DEVICE", ::containsAny, listOf("generic", "Droid4X", "nox")),
        Property("BOARD", ::equalsAny, listOf("unknown")),
        Property("ID", ::equalsAny, listOf("FRF91")),
        Property("SERIAL", ::equalsAny, listOf("null")),
        Property("TAGS", ::equalsAny, listOf("test-keys")),
        Property("USER", ::equalsAny, listOf("android-build"))
    )

    private val emulatorFileMap = mapOf(
        "Genymotion" to listOf("/dev/socket/genyd", "/dev/socket/baseband-genyd"),
        "Nox" to listOf("fstab.nox", "init.nox.rc", "ueventd.nox.rc"),
        "Andy" to listOf("fstab.andy", "ueventd.andy.rc"),
        "x86" to listOf("ueventd.android_x86.rc", "x86.prop", "ueventd.ttVM_x86.rc", "init.ttVM_x86.rc", "fstab.vbox86", "init.vbox86.rc"),
        "QEMU" to listOf("/dev/socket/gemud", "/dev/gemu_pipe")
    )

    fun isEmulator(context: Context): Boolean {
        val score = calculateScore(context)
        Log.d(TAG, "Final emulator score: $score")
        return score >= EMULATOR_SCORE_THRESHOLD
    }

    @SuppressLint("HardwareIds")
    private fun calculateScore(context: Context): Int {
        var score = 0

        val buildProps = mapOf(
            "FINGERPRINT" to Build.FINGERPRINT,
            "MODEL" to Build.MODEL,
            "MANUFACTURER" to Build.MANUFACTURER,
            "HARDWARE" to Build.HARDWARE,
            "PRODUCT" to Build.PRODUCT,
            "BRAND" to Build.BRAND,
            "DEVICE" to Build.DEVICE,
            "BOARD" to Build.BOARD,
            "ID" to Build.ID,
            "SERIAL" to Build.SERIAL,
            "TAGS" to Build.TAGS,
            "USER" to Build.USER
        )

        Log.d(TAG, "================= BUILD PROPERTIES =================")
        buildProps.forEach { (key, value) ->
            Log.d(TAG, "$key = $value")
        }

        Log.d(TAG, "================= SUSPICIOUS MATCHES =================")
        suspiciousBuildProperties.forEach { property ->
            val value = buildProps[property.key] ?: return@forEach
            if (property.matchFunction(value, property.suspiciousValues)) {
                score += property.weight
                Log.w(TAG, "MATCHED: ${property.key} -> $value (matched by ${property.suspiciousValues}, weight=${property.weight})")
            }
        }

        if (!hasAccelerometer(context)) {
            score += 8
            Log.w(TAG, "MATCHED: No accelerometer detected (weight=8)")
        }

        val knownPackages = listOf("com.bluestacks", "com.bignox.app", "com.genymotion.superuser")
        knownPackages.forEach { pkg ->
            if (hasKnownPackage(context, pkg)) {
                score += 10
                Log.w(TAG, "MATCHED: Known emulator package found: $pkg (weight=10)")
            }
        }

        if (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) {
            score += 5
            Log.w(TAG, "MATCHED: Generic BRAND and DEVICE (weight=5)")
        }

        val (fileDetected, emulatorName) = checkEmulatorFiles()
        if (fileDetected) {
            score += 10
            Log.w(TAG, "MATCHED: Emulator file signature detected for $emulatorName (weight=10)")
        }

        Log.d(TAG, "================= FINAL SCORE =================")
        Log.d(TAG, "EMULATOR SCORE: $score")

        return score
    }

    private fun hasAccelerometer(context: Context): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    private fun hasKnownPackage(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkEmulatorFiles(): Pair<Boolean, String?> {
        for ((emulator, files) in emulatorFileMap) {
            for (filePath in files) {
                if (File(filePath).exists()) {
                    return Pair(true, emulator)
                }
            }
        }
        return Pair(false, null)
    }

    private fun containsAny(value: String, candidates: List<String>) =
        candidates.any { value.contains(it, ignoreCase = true) }

    private fun equalsAny(value: String, candidates: List<String>) =
        candidates.any { value.equals(it, ignoreCase = true) }

    private fun startsWithAny(value: String, candidates: List<String>) =
        candidates.any { value.startsWith(it, ignoreCase = true) }

    data class Property(
        val key: String,
        val matchFunction: (String, List<String>) -> Boolean,
        val suspiciousValues: List<String>,
        val weight: Int = 8
    )
}