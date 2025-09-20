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

    suspend fun executeCommand(deviceId: DeviceId?, command: AdbCommand): AdbCommandResult = withContext(Dispatchers.IO) {
        try {
            val adbCommand = buildAdbCommand(deviceId, command)
            val process = createAndStartProcess(adbCommand)
            val output = readProcessOutput(process)
            val isSuccess = waitForProcessCompletion(process)

            AdbCommandResult(
                output = output.trim(),
                isSuccess = isSuccess
            )
        } catch (e: Exception) {
            AdbCommandResult(
                output = "Error: ${e.message}",
                isSuccess = false
            )
        }
    }

    private fun buildAdbCommand(deviceId: DeviceId?, command: AdbCommand): List<String> {
        return buildList {
            add(adbPath)
            if (deviceId != null) {
                add("-s")
                add(deviceId.value)
            }
            add("shell")
            add(command.command)
        }
    }

    private fun createAndStartProcess(adbCommand: List<String>): Process {
        val processBuilder = ProcessBuilder(adbCommand)
        processBuilder.redirectErrorStream(true)
        return processBuilder.start()
    }

    private fun readProcessOutput(process: Process): String {
        return BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            val output = StringBuilder()
            reader.lineSequence().forEach { line ->
                output.appendLine(line)
            }
            output.toString()
        }
    }

    private fun waitForProcessCompletion(process: Process): Boolean {
        val completed = process.waitFor(10, TimeUnit.SECONDS)
        return completed && process.exitValue() == 0
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
