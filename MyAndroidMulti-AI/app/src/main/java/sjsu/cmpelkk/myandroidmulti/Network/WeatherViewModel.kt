package sjsu.cmpelkk.myandroidmulti.Network

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

private const val WeatherAPPID = "2b492c001d57cd5499947bd3d3f9c47b"

class WeatherViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    val _response = MutableLiveData<String>()

    init {
        getWeatherProperties()
    }

    private fun getWeatherProperties() {
        WeatherApi.retrofitService.getProperties("San Jose", WeatherAPPID).enqueue(
            object: Callback<WeatherProperty> {
                override fun onFailure(call: Call<WeatherProperty>, t: Throwable) {
                    //TODO("Not yet implemented")
                    _response.value = "Failure: " + t.message
                }

                override fun onResponse(call: Call<WeatherProperty>, response: Response<WeatherProperty>) {
                    //TODO("Not yet implemented")
                    //_response.value = response.body()
                    _response.value = "Success: ${response.body()?.name} city retrieved; Temperature: ${response.body()?.mainpart?.temp}; Humidity: ${response.body()?.mainpart?.humidity}"
                }

            }
        )
    }
}