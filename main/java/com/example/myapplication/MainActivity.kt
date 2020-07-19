package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.flightmobileapp.Server
import com.example.flightmobileapp.ServersDB
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeOnClickListeners()
        ServersDB.getInstance(application)
        var server = Server()
        server.serverUrl = "http://localhost:42800"

        Thread {
            addToListServersFromDb()
        }.start()

      //  Toast.makeText(applicationContext, "failed in get request", Toast.LENGTH_LONG).show()
    }

    private fun doneClicked(view: View) {
        val urlTextView = findViewById(R.id.url_edit_text) as EditText

        when(view.getId()) {
            R.id.first_server -> urlTextView.setText(first_server.text)
            R.id.second_server -> urlTextView.setText(second_server.text)
            R.id.third_server -> urlTextView.setText(third_server.text)
            R.id.fourth_server -> urlTextView.setText(fourth_server.text)
            R.id.fifth_server -> urlTextView.setText(fifth_server.text)
        }
    }

    private fun initializeOnClickListeners() {
        findViewById<TextView>(R.id.first_server).setOnClickListener {
            doneClicked(it)
        }
        findViewById<TextView>(R.id.second_server).setOnClickListener {
            doneClicked(it)
        }
        findViewById<TextView>(R.id.third_server).setOnClickListener {
            doneClicked(it)
        }
        findViewById<TextView>(R.id.fourth_server).setOnClickListener {
            doneClicked(it)
        }
        findViewById<TextView>(R.id.fifth_server).setOnClickListener {
            doneClicked(it)
        }

        findViewById<Button>(R.id.type_url_button).setOnClickListener {
            val urlTextView = findViewById(R.id.url_edit_text) as EditText

            urlTextView.visibility = if(urlTextView.visibility == TextView.VISIBLE) {
                TextView.INVISIBLE
            } else {
                TextView.VISIBLE
            }
        }

        findViewById<Button>(R.id.connect_button).setOnClickListener {
            connectClick(it)
        }
    }

    private fun addToListServersFromDb() {

        val firstServerTextView = findViewById(R.id.first_server) as TextView
        val secondServerTextView = findViewById(R.id.second_server) as TextView
        val thirdServerTextView = findViewById(R.id.third_server) as TextView
        val fourthServerTextView = findViewById(R.id.fourth_server) as TextView
        val fifthServerTextView = findViewById(R.id.fifth_server) as TextView

        var j = 1

        ServersDB.getInstance(application).serverDao().readServer().forEach() {
            when(j) {
                1 -> firstServerTextView.text = it.serverUrl
                2 -> secondServerTextView.text = it.serverUrl
                3 -> thirdServerTextView.text = it.serverUrl
                4 -> fourthServerTextView.text = it.serverUrl
                5 -> fifthServerTextView.text = it.serverUrl
            }
            println(j)
            println(it.lastConnection)
            j++
        }
    }

    private fun connectClick(view: View) {

        val urlText = findViewById(R.id.url_edit_text) as EditText
        val urlTextString = urlText.text.toString()
        var stringUrl : String = "URL"


        Thread {
            if(!stringUrl.equals(urlTextString)) {
                var server = Server()
                server.serverUrl = urlTextString
                server.lastConnection = System.currentTimeMillis()
                ServersDB.getInstance(application).serverDao().insertOrUpdateServer(server)
                getImage(server.serverUrl)
            }
            else {
                try{
                Toast.makeText(this, "please enter a valid url!", Toast.LENGTH_SHORT).show()
            }catch (e : java.lang.Exception) {
                    print("please enter a valid url!\n")
                }
            }
        }.start()
    }


    private fun getImage(url : String) {
        val gson = GsonBuilder().setLenient().create()
        try {
            var retrofit = Retrofit.Builder().baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson)).build()
            var api = retrofit.create(Api::class.java)
            val body = api.getImg().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    enter(url)
                   }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(applicationContext, "failed in get request", Toast.LENGTH_SHORT).show()
                }

            })

        }catch (e : Exception) {

            val errorText = findViewById(R.id.error_text) as TextView
            errorText.text = "URL is wrong"
            errorText.visibility = if (errorText.visibility != TextView.VISIBLE) {
                TextView.VISIBLE
            } else {
                TextView.VISIBLE
            }

        }
    }

    fun enter(url : String) {
        val intent = Intent(this, secondActivity::class.java)
       intent.putExtra("url",url);
        startActivity(intent)    }
}