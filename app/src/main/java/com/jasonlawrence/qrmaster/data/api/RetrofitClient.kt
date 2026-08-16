package com.jasonlawrence.qrmaster.data.api

import android.content.Context
import com.jasonlawrence.qrmaster.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private var retrofit: Retrofit? = null

    fun getApiService(context: Context): QRApiService {
        if (retrofit == null) {
            val deviceId = DeviceIdManager.getDeviceId(context)

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-Device-ID", deviceId)
                    .build()
                chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                })
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(QRApiService::class.java)
    }
}
