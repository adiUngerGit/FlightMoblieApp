import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServiceBuilder(urlInput:String, timeOutInSeconds:Long) {
    private val url = urlInput
    private val client = OkHttpClient.Builder().callTimeout(timeOutInSeconds,TimeUnit.SECONDS).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(url) // change this IP for testing by your actual machine IP
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    fun<T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }
}