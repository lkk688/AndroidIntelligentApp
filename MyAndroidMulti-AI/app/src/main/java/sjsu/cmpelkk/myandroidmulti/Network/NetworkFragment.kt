package sjsu.cmpelkk.myandroidmulti.Network

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import sjsu.cmpelkk.myandroidmulti.R
import sjsu.cmpelkk.myandroidmulti.databinding.FragmentNetworkBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NetworkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NetworkFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
    private lateinit var binding: FragmentNetworkBinding
    private val viewModel by viewModels<NetworkViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding  = DataBindingUtil.inflate<FragmentNetworkBinding>(inflater, R.layout.fragment_network, container, false)

        //Test send notification
        viewModel.createChannel(getString(R.string.notification_channel_id), getString(R.string.notification_channel_name))
        binding.notifybutton.setOnClickListener {
            viewModel.sendNotification()
        }

        //Access the FCM message
        viewModel.fetchTokens()
        viewModel.createChannel(getString(R.string.fcm_notification_channel_ID), getString(R.string.fcm_notification_channel_name))
        viewModel.subscribeTopic()

        return binding.root
        //return inflater.inflate(R.layout.fragment_network, container, false)
    }


}