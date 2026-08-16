package com.jasonlawrence.qrmaster.data.repository

import android.content.Context
import com.jasonlawrence.qrmaster.data.api.RetrofitClient
import com.jasonlawrence.qrmaster.data.model.QRCode
import com.jasonlawrence.qrmaster.data.model.EmailRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QRRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    suspend fun getQRCodes(): Result<List<QRCode>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getQRCodes()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQRCode(qrCode: QRCode): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.saveQRCode(qrCode)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Save failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteQRCode(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteQRCode(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmail(request: EmailRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.sendEmail(request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Email failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
