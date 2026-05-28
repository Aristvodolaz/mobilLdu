package com.app.mobilldu.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PhotoUploadService {
    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Part("article") sku: RequestBody,
        @Part("marketplace") marketplace: RequestBody
    ): Response<ResponseBody>
}
