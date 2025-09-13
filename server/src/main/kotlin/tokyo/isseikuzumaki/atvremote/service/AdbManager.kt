package tokyo.isseikuzumaki.atvremote.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tokyo.isseikuzumaki.atvremote.shared.AdbCommand
import tokyo.isseikuzumaki.atvremote.shared.AdbCommandResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AdbManager {
    private val adbPath = "adb" // Use adb from system PATH

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
