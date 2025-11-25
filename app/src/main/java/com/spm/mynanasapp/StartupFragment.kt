package com.spm.mynanasapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [StartupFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StartupFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_startup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Function : Animation
        animateEntrance(view)

        // Button : Entrepreneur
        val btnEntrepreneur = view.findViewById<Button>(R.id.btn_login_entrepreneur)
        btnEntrepreneur.setOnClickListener {
            // Navigate to the Login Fragment
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, FragmentLoginEntrepreneur())
//                .addToBackStack(null)
//                .commit()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, FragmentLoginEntrepreneur())
                .addToBackStack(null)
                .commit()
        }

        // Button : User
        val btnUser = view.findViewById<Button>(R.id.btn_login_user)
        btnUser.setOnClickListener {
            // Logic Here
        }
    }

    private fun animateEntrance(view: View) {
        val logo = view.findViewById<View>(R.id.cv_logo_container)
        val title = view.findViewById<View>(R.id.tv_system_name)
        val subtitle = view.findViewById<View>(R.id.tv_instruction)
        val btn1 = view.findViewById<View>(R.id.btn_login_entrepreneur)
        val btn2 = view.findViewById<View>(R.id.btn_login_user)
        val footer = view.findViewById<View>(R.id.tv_footer_copyright)

        val views = listOf(logo, title, subtitle, btn1, btn2, footer)

        for (v in views) {
            v?.alpha = 0f
            v?.translationY = 100f
        }

        logo?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(300)?.start()
        title?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(500)?.start()
        subtitle?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(700)?.start()
        btn1?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(900)?.start()
        btn2?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(1100)?.start()
        footer?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(800)?.setStartDelay(1300)?.start()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment StartupFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StartupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}