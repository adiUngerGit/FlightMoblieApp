package com.example.myapplication

import ServiceBuilder
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RestApiService(urlInput : String) {
    private var url = urlInput
    fun sendCommand(command: Command, onResult: (Int?) -> Unit){
        val retrofit = ServiceBuilder(url, 35).buildService(Api::class.java)
        retrofit.sendCommand(command).enqueue(
            object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if(response.isSuccessful) {
                        onResult(2)
                    }
                    else
                        onResult(1)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onResult(0)
                }
            }
        )
    }
}