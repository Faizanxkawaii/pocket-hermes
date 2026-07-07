package com.pockethermes.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.OutputStreamWriter

class HermesProcess(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _output = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val output: SharedFlow<String> = _output

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var process: Process? = null
    private var stdin: OutputStreamWriter? = null
    private val outputBuffer = mutableListOf<String>()
    private val maxBufferLines = 500

    fun start() {
        if (_isRunning.value) return

        val setupEngine = SetupEngine(context)
        val pythonPath = setupEngine.getPythonPath()
        val hermesScript = setupEngine.getHermesScript()

        // Check if python exists, otherwise use sh fallback
        val usePython = File(pythonPath).exists()

        val pb = if (usePython) {
            ProcessBuilder(pythonPath, "-u", hermesScript)
        } else {
            ProcessBuilder("sh", "-c", "echo 'Python not installed yet. Run setup first.'")
        }

        pb.directory(context.filesDir)
        pb.environment()["PYTHONUNBUFFERED"] = "1"
        pb.redirectErrorStream(true)

        try {
            process = pb.start()
            stdin = OutputStreamWriter(process!!.outputStream)
            _isRunning.value = true

            scope.launch {
                val reader: BufferedReader = process!!.inputStream.bufferedReader()
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        synchronized(outputBuffer) {
                            outputBuffer.add(l)
                            if (outputBuffer.size > maxBufferLines) {
                                val excess = outputBuffer.size - maxBufferLines
                                repeat(excess) { outputBuffer.removeAt(0) }
                            }
                        }
                        _output.emit(l)
                    }
                } catch (_: Exception) {
                } finally {
                    _isRunning.value = false
                    _output.emit("[Process exited]")
                }
            }
        } catch (e: Exception) {
            scope.launch {
                _output.emit("Failed to start: ${e.message}")
            }
        }
    }

    fun sendInput(text: String) {
        scope.launch {
            try {
                stdin?.apply {
                    write(text + "\n")
                    flush()
                }
            } catch (e: Exception) {
                _output.emit("Input error: ${e.message}")
            }
        }
    }

    fun stop() {
        try {
            process?.destroyForcibly()
        } catch (_: Exception) {
            process?.destroy()
        }
        _isRunning.value = false
        process = null
        stdin = null
    }

    fun getBufferedOutput(): List<String> {
        synchronized(outputBuffer) {
            return outputBuffer.toList()
        }
    }
}
