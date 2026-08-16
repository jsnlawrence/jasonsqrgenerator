@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jasonlawrence.qrmaster.ui.generator

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jasonlawrence.qrmaster.data.model.QRCode
import com.jasonlawrence.qrmaster.ui.theme.*
import com.jasonlawrence.qrmaster.viewmodel.QRMode
import com.jasonlawrence.qrmaster.viewmodel.QRViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GeneratorScreen(viewModel: QRViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("EDITOR", "LIBRARY")
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            if (index == 1) {
                                val libraryState by viewModel.libraryState.collectAsState()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title)
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${libraryState.codes.size}")
                                    }
                                }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> EditorTab(viewModel)
                1 -> LibraryTab(viewModel)
            }
        }
    }
}

@Composable
fun EditorTab(viewModel: QRViewModel) {
    val state = viewModel.generatorState
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ModeSelector(state.mode) { viewModel.updateMode(it) }
        Spacer(Modifier.height(16.dp))

        when (state.mode) {
            QRMode.TEXT_URL -> TextUrlInput(state.content) { viewModel.updateContent(it) }
            QRMode.WIFI -> WifiInput(state, viewModel)
            QRMode.CONTACT -> ContactInput(state, viewModel)
            QRMode.JSON -> JsonInput(state.jsonContent) { viewModel.updateJsonContent(it) }
        }

        Spacer(Modifier.height(24.dp))
        AppearanceSection(state, viewModel)
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.generateQRCode() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Generate QR Code", style = MaterialTheme.typography.titleMedium)
        }

        // QR Preview with working Save/Share
        if (state.generatedBitmap != null) {
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = state.generatedBitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(16.dp))

                    // Name input field
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name your QR") },
                        placeholder = { Text("e.g. Company WiFi, My vCard") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.saveToLibrary() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                        OutlinedButton(onClick = {
                            shareQRCode(context, state.generatedBitmap)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun shareQRCode(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_code_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ModeSelector(selected: QRMode, onSelect: (QRMode) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        QRMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RedPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = RedPrimary
                )
            )
        }
    }
}

@Composable
fun TextUrlInput(content: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = content,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        placeholder = { Text("https://example.com") },
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
        )
    )
}

@Composable
fun WifiInput(state: com.jasonlawrence.qrmaster.viewmodel.GeneratorState, viewModel: QRViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.wifiSsid,
            onValueChange = { viewModel.updateWifi(ssid = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Network Name (SSID)") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
            )
        )
        OutlinedTextField(
            value = state.wifiPassword,
            onValueChange = { viewModel.updateWifi(password = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
            )
        )
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = state.wifiSecurity,
                onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("Security") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("WPA", "WEP", "nopass").forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        viewModel.updateWifi(security = option)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun ContactInput(state: com.jasonlawrence.qrmaster.viewmodel.GeneratorState, viewModel: QRViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "Full Name" to state.contactName,
            "Phone" to state.contactPhone,
            "Email" to state.contactEmail,
            "Organization" to state.contactOrg
        ).forEach { (label, value) ->
            OutlinedTextField(
                value = value,
                onValueChange = { newVal ->
                    when (label) {
                        "Full Name" -> viewModel.updateContact(name = newVal)
                        "Phone" -> viewModel.updateContact(phone = newVal)
                        "Email" -> viewModel.updateContact(email = newVal)
                        "Organization" -> viewModel.updateContact(org = newVal)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                )
            )
        }
    }
}

@Composable
fun JsonInput(content: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = content,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().height(200.dp),
        placeholder = { Text("{\"key\": \"value\"}") },
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
        )
    )
}

@Composable
fun AppearanceSection(state: com.jasonlawrence.qrmaster.viewmodel.GeneratorState, viewModel: QRViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("APPEARANCE", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Colors", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorPickerButton("Dots", state.colorDark) { viewModel.updateColorDark(it) }
                        ColorPickerButton("Back", state.colorLight) { viewModel.updateColorLight(it) }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Error Correction", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    val ecLabels = mapOf(
                        ErrorCorrectionLevel.L to "Low (7%)",
                        ErrorCorrectionLevel.M to "Medium (15%)",
                        ErrorCorrectionLevel.Q to "Quartile (25%)",
                        ErrorCorrectionLevel.H to "High (30%)"
                    )
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = ecLabels[state.errorCorrection] ?: "Medium",
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                                focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                            )
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ecLabels.forEach { (level, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    viewModel.updateErrorCorrection(level)
                                    expanded = false
                                })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.size.toString(),
                onValueChange = { it.toIntOrNull()?.let { s -> viewModel.updateSize(s) } },
                modifier = Modifier.width(120.dp),
                label = { Text("Size (px)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary, unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated
                )
            )
        }
    }
}

// ============================================================
// VISUAL COLOR PICKER
// ============================================================

@Composable
fun ColorPickerButton(label: String, currentColor: String, onColorSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val color = try { Color(android.graphics.Color.parseColor(currentColor)) } catch (e: Exception) { Color.White }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                .clickable { showDialog = true }
        )
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }

    if (showDialog) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { showDialog = false },
            onColorPicked = { hex ->
                onColorSelected(hex)
                showDialog = false
            }
        )
    }
}

@Composable
fun ColorPickerDialog(initialColor: String, onDismiss: () -> Unit, onColorPicked: (String) -> Unit) {
    val initArgb = try { android.graphics.Color.parseColor(initialColor) } catch (e: Exception) { android.graphics.Color.WHITE }
    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initArgb, initHsv)

    var hue by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var value by remember { mutableFloatStateOf(initHsv[2]) }

    val selectedColor = Color.hsv(hue, saturation, value)
    val hexString = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Hue bar
                Text("Hue", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = (0..360 step 30).map { Color.hsv(it.toFloat(), 1f, 1f) }
                            )
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                )

                Spacer(Modifier.height(16.dp))

                // Saturation bar
                Text("Saturation", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.hsv(hue, 0f, value), Color.hsv(hue, 1f, value))
                            )
                        )
                        .pointerInput(hue, value) {
                            detectTapGestures { offset ->
                                saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue, value) {
                            detectDragGestures { change, _ ->
                                saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                )

                Spacer(Modifier.height(16.dp))

                // Brightness bar
                Text("Brightness", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.hsv(hue, saturation, 0f), Color.hsv(hue, saturation, 1f))
                            )
                        )
                        .pointerInput(hue, saturation) {
                            detectTapGestures { offset ->
                                value = (offset.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(hue, saturation) {
                            detectDragGestures { change, _ ->
                                value = (change.position.x / size.width).coerceIn(0f, 1f)
                            }
                        }
                )

                Spacer(Modifier.height(16.dp))

                // Preview swatch + hex
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(2.dp, DarkBorder, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(hexString, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                }

                Spacer(Modifier.height(12.dp))

                // Quick presets
                Text("Presets", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf("#FFFFFF", "#000000", "#E53935", "#FF9800", "#4CAF50", "#2196F3", "#9C27B0", "#FFEB3B")
                    presets.forEach { preset ->
                        val presetColor = Color(android.graphics.Color.parseColor(preset))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(1.dp, DarkBorder, CircleShape)
                                .clickable { onColorPicked(preset) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorPicked(hexString) },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ============================================================
// LIBRARY TAB
// ============================================================

@Composable
fun LibraryTab(viewModel: QRViewModel) {
    val libraryState by viewModel.libraryState.collectAsState()

    when {
        libraryState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
        }
        libraryState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${libraryState.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.loadLibrary() }) { Text("Retry") }
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(libraryState.codes, key = { it.id }) { code ->
                    LibraryCard(code, viewModel)
                }
            }
        }
    }
}

@Composable
fun LibraryCard(code: QRCode, viewModel: QRViewModel) {
    val badgeColor = when (code.type.lowercase()) {
        "json" -> BadgeJson
        "wifi" -> BadgeWifi
        "contact" -> BadgeContact
        else -> BadgeText
    }
    val dateStr = SimpleDateFormat("M/d/yyyy", Locale.US).format(Date(code.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.loadFromLibrary(code) },
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(code.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = badgeColor) {
                        Text(code.type.uppercase(), style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = { viewModel.deleteFromLibrary(code.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                code.content.take(80) + if (code.content.length > 80) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(dateStr, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
    }
}
