package com.jasonlawrence.qrmaster.viewmodel

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.jasonlawrence.qrmaster.data.model.EmailRequest
import com.jasonlawrence.qrmaster.data.model.QRCode
import com.jasonlawrence.qrmaster.data.model.QROptions
import com.jasonlawrence.qrmaster.data.repository.QRRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class GeneratorState(
    val mode: QRMode = QRMode.TEXT_URL,
    val content: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    val jsonContent: String = "",
    val colorDark: String = "#FFFFFF",
    val colorLight: String = "#000000",
    val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
    val size: Int = 300,
    val generatedBitmap: Bitmap? = null,
    val name: String = ""
)

enum class QRMode(val label: String) {
    TEXT_URL("Text / URL"),
    WIFI("WiFi"),
    CONTACT("Contact Card"),
    JSON("JSON")
}

data class LibraryState(
    val codes: List<QRCode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class QRViewModel : ViewModel() {

    private val repository = QRRepository()

    var generatorState by mutableStateOf(GeneratorState())
        private set

    private val _libraryState = MutableStateFlow(LibraryState())
    val libraryState: StateFlow<LibraryState> = _libraryState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadLibrary()
    }

    // --- Generator Actions ---

    fun updateMode(mode: QRMode) {
        generatorState = generatorState.copy(mode = mode)
    }

    fun updateContent(content: String) {
        generatorState = generatorState.copy(content = content)
    }

    fun updateWifi(ssid: String? = null, password: String? = null, security: String? = null) {
        generatorState = generatorState.copy(
            wifiSsid = ssid ?: generatorState.wifiSsid,
            wifiPassword = password ?: generatorState.wifiPassword,
            wifiSecurity = security ?: generatorState.wifiSecurity
        )
    }

    fun updateContact(name: String? = null, phone: String? = null, email: String? = null, org: String? = null) {
        generatorState = generatorState.copy(
            contactName = name ?: generatorState.contactName,
            contactPhone = phone ?: generatorState.contactPhone,
            contactEmail = email ?: generatorState.contactEmail,
            contactOrg = org ?: generatorState.contactOrg
        )
    }

    fun updateJsonContent(json: String) {
        generatorState = generatorState.copy(jsonContent = json)
    }

    fun updateColorDark(color: String) {
        generatorState = generatorState.copy(colorDark = color)
    }

    fun updateColorLight(color: String) {
        generatorState = generatorState.copy(colorLight = color)
    }

    fun updateErrorCorrection(level: ErrorCorrectionLevel) {
        generatorState = generatorState.copy(errorCorrection = level)
    }

    fun updateSize(size: Int) {
        generatorState = generatorState.copy(size = size)
    }

    fun updateName(name: String) {
        generatorState = generatorState.copy(name = name)
    }

    fun generateQRCode() {
        val content = buildQRContent() ?: return
        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to generatorState.errorCorrection,
                EncodeHintType.MARGIN to 2
            )
            val writer = QRCodeWriter()
            val size = generatorState.size
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

            val darkColor = parseColor(generatorState.colorDark)
            val lightColor = parseColor(generatorState.colorLight)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
                }
            }

            generatorState = generatorState.copy(generatedBitmap = bitmap)
        } catch (e: Exception) {
            _snackbarMessage.value = "Generation failed: ${e.message}"
        }
    }

    fun saveToLibrary() {
        val bitmap = generatorState.generatedBitmap ?: return
        val content = buildQRContent() ?: return
        val name = generatorState.name.ifBlank {
            "QR ${java.text.SimpleDateFormat("h:mm:ss a", java.util.Locale.US).format(java.util.Date())}"
        }

        val qrCode = QRCode(
            id = UUID.randomUUID().toString(),
            name = name,
            type = generatorState.mode.name.lowercase(),
            content = content,
            options = QROptions(
                colorDark = generatorState.colorDark,
                colorLight = generatorState.colorLight,
                errorCorrectionLevel = when (generatorState.errorCorrection) {
                    ErrorCorrectionLevel.L -> "L"
                    ErrorCorrectionLevel.M -> "M"
                    ErrorCorrectionLevel.Q -> "Q"
                    ErrorCorrectionLevel.H -> "H"
                },
                width = generatorState.size
            ),
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveQRCode(qrCode).fold(
                onSuccess = {
                    _snackbarMessage.value = "Saved to library!"
                    loadLibrary()
                },
                onFailure = { _snackbarMessage.value = "Save failed: ${it.message}" }
            )
        }
    }

    // --- Library Actions ---

    fun loadLibrary() {
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(isLoading = true)
            repository.getQRCodes().fold(
                onSuccess = { codes ->
                    _libraryState.value = LibraryState(codes = codes)
                },
                onFailure = { e ->
                    _libraryState.value = LibraryState(error = e.message)
                }
            )
        }
    }

    fun deleteFromLibrary(id: String) {
        viewModelScope.launch {
            repository.deleteQRCode(id).fold(
                onSuccess = {
                    _snackbarMessage.value = "Deleted"
                    loadLibrary()
                },
                onFailure = { _snackbarMessage.value = "Delete failed: ${it.message}" }
            )
        }
    }

    fun loadFromLibrary(qrCode: QRCode) {
        // Reload a saved QR code into the generator
        val ecLevel = when (qrCode.options.errorCorrectionLevel) {
            "L" -> ErrorCorrectionLevel.L
            "Q" -> ErrorCorrectionLevel.Q
            "H" -> ErrorCorrectionLevel.H
            else -> ErrorCorrectionLevel.M
        }
        val mode = when (qrCode.type) {
            "wifi" -> QRMode.WIFI
            "contact" -> QRMode.CONTACT
            "json" -> QRMode.JSON
            else -> QRMode.TEXT_URL
        }
        generatorState = GeneratorState(
            mode = mode,
            content = if (mode == QRMode.TEXT_URL) qrCode.content else "",
            jsonContent = if (mode == QRMode.JSON) qrCode.content else "",
            colorDark = qrCode.options.colorDark,
            colorLight = qrCode.options.colorLight,
            errorCorrection = ecLevel,
            size = qrCode.options.width,
            name = qrCode.name
        )
        generateQRCode()
    }

    // --- Email ---

    fun sendEmail(receiver: String, subject: String, body: String, base64Image: String) {
        viewModelScope.launch {
            val request = EmailRequest(receiver, subject, body, base64Image)
            repository.sendEmail(request).fold(
                onSuccess = { _snackbarMessage.value = "Email sent!" },
                onFailure = { _snackbarMessage.value = "Email failed: ${it.message}" }
            )
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // --- Helpers ---

    private fun buildQRContent(): String? {
        return when (generatorState.mode) {
            QRMode.TEXT_URL -> generatorState.content.ifBlank { null }
            QRMode.WIFI -> {
                val ssid = generatorState.wifiSsid
                val pass = generatorState.wifiPassword
                val sec = generatorState.wifiSecurity
                if (ssid.isBlank()) null
                else "WIFI:T:$sec;S:$ssid;P:$pass;;"
            }
            QRMode.CONTACT -> {
                val name = generatorState.contactName
                if (name.isBlank()) null
                else buildString {
                    append("BEGIN:VCARD\nVERSION:3.0\n")
                    append("FN:$name\n")
                    if (generatorState.contactPhone.isNotBlank())
                        append("TEL:${generatorState.contactPhone}\n")
                    if (generatorState.contactEmail.isNotBlank())
                        append("EMAIL:${generatorState.contactEmail}\n")
                    if (generatorState.contactOrg.isNotBlank())
                        append("ORG:${generatorState.contactOrg}\n")
                    append("END:VCARD")
                }
            }
            QRMode.JSON -> generatorState.jsonContent.ifBlank { null }
        }
    }

    private fun parseColor(hex: String): Int {
        return try {
            AndroidColor.parseColor(hex)
        } catch (e: Exception) {
            AndroidColor.BLACK
        }
    }
}
