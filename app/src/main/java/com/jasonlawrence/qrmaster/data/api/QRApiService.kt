package com.jasonlawrence.qrmaster.data.api

import com.jasonlawrence.qrmaster.data.model.QRCode
import com.jasonlawrence.qrmaster.data.model.EmailRequest
import com.jasonlawrence.qrmaster.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.*

interface QRApiService {

    @GET("api/qrcodes")
    suspend fun getQRCodes(): Response<List<QRCode>>

    @POST("api/qrcodes")
    suspend fun saveQRCode(@Body qrCode: QRCode): Response<ApiResponse>

    @DELETE("api/qrcodes/{id}")
    suspend fun deleteQRCode(@Path("id") id: String): Response<ApiResponse>

    @POST("api/send_email")
    suspend fun sendEmail(@Body request: EmailRequest): Response<ApiResponse>
}
