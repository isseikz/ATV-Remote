package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.AdbCommandResult
import tokyo.isseikuzumaki.atvremote.shared.AdbDevice
import tokyo.isseikuzumaki.atvremote.shared.DeviceId
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AdbManager {
    private val adbPath = "adb" // Use adb from system PATH

    suspend fun devices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "devices").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()

            val lines = output.split("\n").filter { it.isNotBlank() }
            return@withContext lines.drop(1).mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size == 2) {
                    AdbDevice(
                        id = DeviceId(parts[0]),
                        name = parts[0],
                        connected = parts[1] == "device"
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun executeCommand(command: AdbCommand): AdbCommandResult = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(adbPath, "shell", command.command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val exitCode = process.waitFor(10, TimeUnit.SECONDS)
            val isSuccess = exitCode && process.exitValue() == 0

            AdbCommandResult(
                output = output.toString().trim(),
                isSuccess = isSuccess
            )
        } catch (e: Exception) {
            AdbCommandResult(
                output = "Error: ${e.message}",
                isSuccess = false
            )
        }
    }

    suspend fun checkAdbConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "devices").start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()

            // Check if any devices are connected
            val lines = output.split("\n")
            return@withContext lines.any { line ->
                line.contains("\tdevice") || line.contains("\tunauthorized")
            }
        } catch (e: Exception) {
            false
        }
    }
}
