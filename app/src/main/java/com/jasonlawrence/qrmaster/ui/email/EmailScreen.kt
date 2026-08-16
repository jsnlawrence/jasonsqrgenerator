package com.jasonlawrence.qrmaster.ui.email

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jasonlawrence.qrmaster.ui.theme.*
import com.jasonlawrence.qrmaster.viewmodel.QRViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun EmailScreen(viewModel: QRViewModel) {
    var receiver by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Your QR Code from Jason's QR Generator") }
    var body by remember { mutableStateOf("Here is your QR code. Scan it directly from this email!") }
    var isSending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val bitmap = viewModel.generatorState.generatedBitmap
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
            isSending = false
            sendResult = it
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Email QR Code", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))

            if (bitmap == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No QR code generated yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Go to the Generate tab first to create a QR code, then come back here to email it.", color = TextSecondary)
                    }
                }
            } else {
                Text("Send the generated QR code as an inline email attachment.", color = TextSecondary)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = receiver,
                    onValueChange = { receiver = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recipient Email") },
                    placeholder = { Text("name@example.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    label = { Text("Message") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                    )
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (receiver.isNotBlank()) {
                            isSending = true
                            sendResult = null
                            val base64 = bitmapToBase64(bitmap)
                            viewModel.sendEmail(receiver, subject, body, base64)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = receiver.isNotBlank() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sending...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Send Email", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Success/result feedback
                if (sendResult != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sendResult!!.contains("sent", ignoreCase = true))
                                Color(0xFF1B5E20).copy(alpha = 0.3f)
                            else Color(0xFFB71C1C).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (sendResult!!.contains("sent", ignoreCase = true)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(sendResult!!, color = TextPrimary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val bytes = stream.toByteArray()
    return "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}
