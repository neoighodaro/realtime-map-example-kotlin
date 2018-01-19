package com.example.android.realtimemapkotlin

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by idorenyin on 1/14/18.
 */
interface ApiInterface {

    @POST("/simulate")
    fun sendCoordinates(@Body coordinates: RequestBody): Call<String>


}