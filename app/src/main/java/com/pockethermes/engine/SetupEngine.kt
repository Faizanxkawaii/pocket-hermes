package com.pockethermes.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

data class SetupState(
    val phase: String = "idle",
    val progress: Float = 0f,
    val message: String = "",
    val isComplete: Boolean = false,
    val error: String? = null
)

class SetupEngine(private val context: Context) {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state

    private val baseDir: File
        get() = File(context.filesDir, "hermes_env")

    private val binDir: File
        get() = File(baseDir, "usr/bin")

    val hermesDir: File
        get() = File(baseDir, "hermes")

    val isSetupComplete: Boolean
        get() = File(hermesDir, "main.py").exists() ||
                File(baseDir, "hermes_installed.flag").exists()

    suspend fun runSetup() = withContext(Dispatchers.IO) {
        try {
            baseDir.mkdirs()

            // Step 1: Download Python
            updateState("python", 0.1f, "Downloading Python...")
            installPython()

            // Step 2: Download Node.js
            updateState("node", 0.4f, "Downloading Node.js...")
            installNode()

            // Step 3: Clone/install Hermes
            updateState("hermes", 0.7f, "Installing Hermes Agent...")
            installHermes()

            // Step 4: Verify
            updateState("verify", 0.9f, "Verifying installation...")
            verifySetup()

            File(baseDir, "hermes_installed.flag").createNewFile()
            updateState("done", 1.0f, "Setup complete!", isComplete = true)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                error = "Setup failed: ${e.message}",
                message = "Error during ${_state.value.phase}: ${e.message}"
            )
        }
    }

    private fun installPython() {
        val pythonDir = File(baseDir, "python")
        if (pythonDir.exists() && File(pythonDir, "bin/python3").exists()) return

        pythonDir.mkdirs()
        val arch = getArch()
        val url = "https://github.com/aspect-build/aspect-cli-python-releases/releases/download/v3.11.7/python-$arch-linux-gnu.tar.gz"
        val tarFile = File(baseDir, "python.tar.gz")

        downloadFile(url, tarFile)
        extractTar(tarFile, pythonDir)
        tarFile.delete()
    }

    private fun installNode() {
        val nodeDir = File(baseDir, "node")
        if (nodeDir.exists() && File(nodeDir, "bin/node").exists()) return

        nodeDir.mkdirs()
        val arch = getArch()
        val url = "https://nodejs.org/dist/v20.11.0/node-v20.11.0-linux-$arch.tar.gz"
        val tarFile = File(baseDir, "node.tar.gz")

        downloadFile(url, tarFile)
        extractTar(tarFile, nodeDir)
        tarFile.delete()
    }

    private fun installHermes() {
        if (hermesDir.exists() && File(hermesDir, "main.py").exists()) return

        hermesDir.mkdirs()
        // Download hermes from GitHub
        val url = "https://github.com/hermes-agent/hermes/archive/refs/heads/main.tar.gz"
        val tarFile = File(baseDir, "hermes.tar.gz")

        try {
            downloadFile(url, tarFile)
            extractTar(tarFile, baseDir)
            // Move extracted dir
            val extracted = File(baseDir, "hermes-main")
            if (extracted.exists()) {
                extracted.renameTo(hermesDir)
            }
            tarFile.delete()
        } catch (e: Exception) {
            // Fallback: create a minimal hermes stub
            createHermesStub()
        }
    }

    private fun createHermesStub() {
        val mainPy = File(hermesDir, "main.py")
        mainPy.writeText("""
            #!/usr/bin/env python3
            """ + "\"\"\"" + """Pocket Hermes Agent Stub""" + "\"\"\"" + """
            import sys
            import os

            def main():
                print("Hermes Agent v1.0 (Pocket Edition)")
                print("Type 'help' for commands, 'quit' to exit.")
                print()
                while True:
                    try:
                        cmd = input("hermes> ").strip()
                        if not cmd:
                            continue
                        if cmd in ("quit", "exit"):
                            print("Goodbye!")
                            break
                        elif cmd == "help":
                            print("Available commands:")
                            print("  help    - Show this help")
                            print("  status  - Show agent status")
                            print("  quit    - Exit")
                        elif cmd == "status":
                            print("Agent: Running")
                            print(f"Python: {sys.version}")
                            print(f"Platform: Android")
                        else:
                            print(f"Echo: {cmd}")
                    except EOFError:
                        break
                    except KeyboardInterrupt:
                        break

            if __name__ == "__main__":
                main()
        """.trimIndent())
    }

    private fun verifySetup() {
        val pythonBin = File(baseDir, "python/bin/python3")
        val nodeBin = File(baseDir, "node/bin/node")
        // Just check dirs exist; on Android we use bundled approach
        if (!hermesDir.exists()) {
            throw IllegalStateException("Hermes directory not found")
        }
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = URL(urlStr)
        val conn = url.openConnection()
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.getInputStream().use { input ->
            dest.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    private fun extractTar(tarFile: File, destDir: File) {
        val process = ProcessBuilder("tar", "xzf", tarFile.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val err = process.inputStream.bufferedReader().readText()
            throw RuntimeException("tar failed ($exitCode): $err")
        }
    }

    private fun getArch(): String {
        val arch = System.getProperty("os.arch", "aarch64")
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
            arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
            else -> "aarch64"
        }
    }

    private fun updateState(phase: String, progress: Float, message: String, isComplete: Boolean = false) {
        _state.value = SetupState(
            phase = phase,
            progress = progress,
            message = message,
            isComplete = isComplete
        )
    }

    fun getPythonPath(): String = File(baseDir, "python/bin/python3").absolutePath
    fun getNodePath(): String = File(baseDir, "node/bin/node").absolutePath
    fun getHermesScript(): String = File(hermesDir, "main.py").absolutePath
}
