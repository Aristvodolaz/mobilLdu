package com.app.mobilldu.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface PhotoUploadService {
    @Multipart
    @POST("api/photos")
    suspend fun uploadPhotos(
        @Part photos: List<MultipartBody.Part>,
        @Part("article") article: RequestBody,
        @Part("marketplace") marketplace: RequestBody
    ): Response<ResponseBody>

    @GET("api/photos")
    suspend fun getPhotos(): Response<List<ServerPhotoDto>>

    @DELETE("api/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: Long): Response<ResponseBody>

    @PUT("api/photos/marketplace")
    suspend fun updateMarketplace(@Body body: Map<String, String>): Response<ResponseBody>
}

