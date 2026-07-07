package com.pockethermes.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pockethermes.engine.HermesProcess
import com.pockethermes.engine.HermesService

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hermesProcess by remember { mutableStateOf<HermesProcess?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as HermesService.HermesBinder
                hermesProcess = binder.getService().hermesProcess
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                hermesProcess = null
                isBound = false
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, HermesService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        onDispose {
            if (isBound) {
                context.unbindService(connection)
            }
        }
    }

    if (hermesProcess != null) {
        TerminalView(
            hermesProcess = hermesProcess!!,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Starting Hermes...",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun TerminalView(
    hermesProcess: HermesProcess,
    modifier: Modifier = Modifier
) {
    val output by hermesProcess.output.collectAsState(initial = "")
    val isRunning by hermesProcess.isRunning.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val outputLines = remember { mutableStateOf(hermesProcess.getBufferedOutput()) }

    // Collect new output
    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            outputLines.value = outputLines.value + output
            // Scroll to bottom
            if (outputLines.value.isNotEmpty()) {
                listState.animateScrollToItem(outputLines.value.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(8.dp)
    ) {
        // Terminal header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hermes Terminal",
                color = Color(0xFF00FF88),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isRunning) "● running" else "○ stopped",
                color = if (isRunning) Color(0xFF00FF88) else Color(0xFFFF4444),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Terminal output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0D0D0D))
                .padding(8.dp)
        ) {
            items(outputLines.value) { line ->
                Text(
                    text = line,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1A1A2E),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "❯ ",
                color = Color(0xFF00FF88),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp),
                placeholder = {
                    Text(
                        text = "Type command...",
                        color = Color(0xFF666666),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFCCCCCC),
                    unfocusedTextColor = Color(0xFFCCCCCC),
                    cursorColor = Color(0xFF00FF88),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        hermesProcess.sendInput(inputText)
                        inputText = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF00FF88)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }

            IconButton(
                onClick = {
                    hermesProcess.stop()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFFFF4444)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop"
                )
            }
        }
    }
}
