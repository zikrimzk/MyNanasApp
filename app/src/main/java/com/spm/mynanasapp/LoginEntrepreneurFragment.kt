package com.spm.mynanasapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentLoginEntrepreneur.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentLoginEntrepreneur : Fragment() {
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
        return inflater.inflate(R.layout.fragment_login_entrepreneur, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animateEntrance(view)

        // Initialization Views
        val btnLogin = view.findViewById<Button>(R.id.btn_do_login)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)
        val tvSignUpAction = view.findViewById<TextView>(R.id.tv_signup_action)

        // Button : Login
        btnLogin.setOnClickListener {

            // Validation Logic

            // Create an Intent to switch from the current Activity to the New Portal Activity
            val intent =
                android.content.Intent(requireActivity(), EntrepreneurPortalActivity::class.java)

            // Start the new Activity
            startActivity(intent)

            // Close the Login Activity
            requireActivity().finish()
        }

        // Link : Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(context, "Forgot Password Clicked", Toast.LENGTH_SHORT).show()
        }

        // Link : Sign Up
        tvSignUpAction.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, SignUpEntrepreneurFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun animateEntrance(view: View) {
        val title = view.findViewById<View>(R.id.tv_login_title)
        val inputUser = view.findViewById<View>(R.id.til_username)
        val inputPass = view.findViewById<View>(R.id.til_password)
        val remember = view.findViewById<View>(R.id.cb_remember_me)
        val forgot = view.findViewById<View>(R.id.tv_forgot_password)
        val btnLogin = view.findViewById<View>(R.id.btn_do_login)
        val signUpText = view.findViewById<View>(R.id.tv_signup_text)
        val signUpAction = view.findViewById<View>(R.id.tv_signup_action)

        val views = listOf(title, inputUser, inputPass, remember, forgot, btnLogin, signUpText, signUpAction)

        for (v in views) {
            v?.alpha = 0f
            v?.translationY = 50f
        }

        title?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()

        inputUser?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()
        inputPass?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(500)?.start()

        remember?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()
        forgot?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()

        btnLogin?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(700)?.start()
        signUpText?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(800)?.start()
        signUpAction?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(800)?.start()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentLoginEntrepreneur.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentLoginEntrepreneur().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}