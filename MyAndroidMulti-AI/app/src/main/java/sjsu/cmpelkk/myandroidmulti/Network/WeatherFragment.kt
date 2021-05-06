package sjsu.cmpelkk.myandroidmulti.Network

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import sjsu.cmpelkk.myandroidmulti.R
import sjsu.cmpelkk.myandroidmulti.databinding.WeatherFragmentBinding

class WeatherFragment : Fragment() {

    companion object {
        fun newInstance() = WeatherFragment()
    }

    private lateinit var viewModel: WeatherViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<WeatherFragmentBinding>(inflater, R.layout.weather_fragment,container,false)
        viewModel = ViewModelProviders.of(this).get(WeatherViewModel::class.java)

        viewModel._response.observe(viewLifecycleOwner, Observer { newresponse ->
            binding.weathertextView.text = newresponse.toString() //display the raw json data
        })


            return binding.root
        //return inflater.inflate(R.layout.weather_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WeatherViewModel::class.java)
        // TODO: Use the ViewModel
    }

}