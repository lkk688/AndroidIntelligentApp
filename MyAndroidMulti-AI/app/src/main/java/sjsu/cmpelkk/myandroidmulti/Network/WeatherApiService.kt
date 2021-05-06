package sjsu.cmpelkk.myandroidmulti.Network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL = "https://api.openweathermap.org"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

//private val retrofit = Retrofit.Builder()
//    .addConverterFactory(ScalarsConverterFactory.create())
//    .baseUrl(BASE_URL)
//    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface WeatherApiService {
    @GET("/data/2.5/weather")//realestate Retrofit appends the endpoint to the base URL
    fun getProperties(@Query("q") city: String, @Query("appid") apiKey: String):
        Call<WeatherProperty>
        //Call<String>

}

object WeatherApi {
    val retrofitService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}


//class WeatherApiService {
//}