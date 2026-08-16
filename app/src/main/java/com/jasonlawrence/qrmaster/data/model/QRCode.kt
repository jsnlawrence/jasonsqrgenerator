package com.jasonlawrence.qrmaster.data.model

import com.google.gson.annotations.SerializedName

data class QRCode(
    val id: String,
    val name: String,
    val type: String,          // "text", "wifi", "contact", "json"
    val content: String,
    val options: QROptions,
    val createdAt: Long
)

data class QROptions(
    val colorDark: String = "#000000",
    val colorLight: String = "#FFFFFF",
    val errorCorrectionLevel: String = "M",
    val margin: Int = 2,
    val width: Int = 300,
    val logoDataUrl: String? = null,
    val text: String = ""
)

data class EmailRequest(
    val receiver: String,
    val subject: String,
    val body: String,
    @SerializedName("image_data")
    val imageData: String
)

data class ApiResponse(
    val status: String,
    val message: String? = null
)
