package sjsu.cmpelkk.myandroidmulti

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import sjsu.cmpelkk.myandroidmulti.databinding.FragmentQuestionBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [QuestionFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [QuestionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QuestionFragment : Fragment() {
    // TODO: Rename and change types of parameters
//    private var param1: String? = null
//    private var param2: String? = null
//    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_question, container, false)

        val binding = DataBindingUtil.inflate<FragmentQuestionBinding>(inflater,
            R.layout.fragment_question,container,false)

        binding.button2.setOnClickListener {view: View ->
            val checkedId = binding.questionRadioGroup.checkedRadioButtonId
            if (-1 != checkedId) {
                var answerIndex = 0
                when (checkedId) {
                    R.id.firstanswerradioButton -> answerIndex = 1
                    R.id.secondasnwerradioButton -> answerIndex = 2
                }
                if (answerIndex ==1)
                {
                    Navigation.findNavController(view).navigate(R.id.action_questionFragment_to_answerOneFragment2)
                }else {
                    Navigation.findNavController(view).navigate(R.id.action_questionFragment_to_answerTwoFragment2)
                }
            }

        }

        return binding.root
    }
}
