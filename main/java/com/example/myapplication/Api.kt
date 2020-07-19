package com.example.myapplication

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface Api {

    @POST("/api/command")
    fun sendCommand(@Body userData: Command ): Call<ResponseBody>

    @GET("/screenshot")
    fun getImg(): Call<ResponseBody>

}

data class Command  (
    @SerializedName("rudder") var rudder: Double?,
    @SerializedName("aileron") var aileron: Double?,
    @SerializedName("elevator") var elevator: Double?,
    @SerializedName("throttle") var throttle: Double?

)