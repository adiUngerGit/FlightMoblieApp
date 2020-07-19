package com.example.myapplication
import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_second.*
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception

class secondActivity : Activity() {

    private var lastX = 0.0;
    private var lastY = 0.0;
    private var lastRudder = 0.0;
    private var lastThrottle = 0.0;
    var send = Command(
        rudder = 0.0,
        aileron = 0.0,
        elevator = 0.0,
        throttle = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        var url = getIntent().getStringExtra("url").toString()

        GlobalScope.launch {
            while (true) {
                delay(1000)
                getImage(url)
            }
        }

        val js = findViewById<JoystickAndSlidersView>(R.id.joystickAndSlidersView)
        js.onBottomSliderValueChanged = {
            var value = js.bottomKnobValue
            if (value > lastRudder + 0.01 || value < lastRudder - 0.01) {
                lastRudder = value.toDouble();
                send.rudder = lastRudder;
                sendCommand(url)
            }
        }

        js.onLeftSliderValueChanged = {
            var value = js.leftKnobValue
            if (value > lastThrottle + 0.01 || value < lastThrottle - 0.01) {
                lastThrottle = value.toDouble();
                send.throttle = lastThrottle;
                sendCommand(url)
            }
        }

        js.onJoystickValueChange = {

            var onePercent = 0.01
            var aileron = js.joystickHorizontalValue.toDouble()
            var elevator = js.joystickVerticalValue.toDouble()
            if ((((elevator > lastY + onePercent || elevator < lastY - onePercent) ||
                        (aileron > lastX + onePercent || aileron < lastX - onePercent) ) && (aileron != 0.0 && elevator != 0.0))
            ) {
                lastY = elevator
                lastX = aileron
                send.elevator = lastY;
                send.aileron = lastX;
                sendCommand(url)
            }
        }
    }


    private fun sendCommand(url : String) {
            val api = RestApiService(url)
            api.sendCommand(send) {
                if (it == 0 || it == 1) {
                    try {
                    Toast.makeText(this, "failed in get request", Toast.LENGTH_SHORT).show()
                } catch (e : Exception) {
                    println("error")}
                }
            }
       }


    private fun getImage(url : String) {
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder().baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
        var api = retrofit.create(Api::class.java)
        api.getImg().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if(!response.isSuccessful()) {
                    try {
                    Toast.makeText(applicationContext, "return bad request", Toast.LENGTH_SHORT).show();
                }catch (e : Exception) {
                    println("error")}
                }
                val I = response.body()?.byteStream()
                val B = BitmapFactory.decodeStream(I)
                runOnUiThread {
                   imageView.setImageBitmap(B)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                try {
                Toast.makeText(applicationContext, "failed in get image", Toast.LENGTH_SHORT).show()
            }catch (e : Exception) {
                println("error")}
            }
        })
    }
}










