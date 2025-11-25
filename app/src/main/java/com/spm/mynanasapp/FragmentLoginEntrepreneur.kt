package com.spm.mynanasapp

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

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

        // Button : Login
        btnLogin.setOnClickListener {
            // Here you will eventually get the text from et_username and et_password
            Toast.makeText(context, "Logging in...", Toast.LENGTH_SHORT).show()
        }

        // Link : Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(context, "Forgot Password Clicked", Toast.LENGTH_SHORT).show()
        }

        // Link : Sign Up
        setupSignUpText(view)

    }

    private fun setupSignUpText(view: View) {
        val tvSignUp = view.findViewById<TextView>(R.id.tv_signup_prompt)

        // The full text string
        val fullText = "Don't have an account? Sign Up"

        // The specific word we want to color/click
        val targetWord = "Sign Up"

        // Create the SpannableString
        val spannableString = SpannableString(fullText)

        // Calculate where the word "Sign Up" starts and ends
        val startIndex = fullText.indexOf(targetWord)
        val endIndex = startIndex + targetWord.length

        // --- PART A: Make it Clickable ---
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // CODE: What happens when they click "Sign Up"
                Toast.makeText(context, "Redirecting to Registration...", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                // Remove the default underline
                ds.isUnderlineText = false
                // Set the text to Bold
                ds.isFakeBoldText = true
            }
        }

        spannableString.setSpan(
            clickableSpan,
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // --- PART B: Make it Orange ---
        val orangeColor = ContextCompat.getColor(requireContext(), R.color.gov_orange_primary)
        val colorSpan = ForegroundColorSpan(orangeColor)

        spannableString.setSpan(
            colorSpan,
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Apply the styled text to the TextView
        tvSignUp.text = spannableString

        // CRITICAL: This line makes the ClickableSpan actually work
        tvSignUp.movementMethod = LinkMovementMethod.getInstance()

        tvSignUp.highlightColor = Color.TRANSPARENT
    }

    private fun animateEntrance(view: View) {
        // Find all the views we want to animate
        val title = view.findViewById<View>(R.id.tv_login_title)
        val inputUser = view.findViewById<View>(R.id.til_username)
        val inputPass = view.findViewById<View>(R.id.til_password)
        val remember = view.findViewById<View>(R.id.cb_remember_me)
        val forgot = view.findViewById<View>(R.id.tv_forgot_password)
        val btnLogin = view.findViewById<View>(R.id.btn_do_login)
        val signUp = view.findViewById<View>(R.id.tv_signup_prompt)

        // Group them into a list for cleaner code
        val views = listOf(title, inputUser, inputPass, remember, forgot, btnLogin, signUp)

        // 1. Prepare: Hide them and push them down slightly
        for (v in views) {
            v?.alpha = 0f
            v?.translationY = 50f // Less distance than startup page for a tighter feel
        }

        // Animate: Bring them back one by one
        title?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()

        // Inputs slide in together
        inputUser?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()
        inputPass?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(500)?.start()

        // Options appear
        remember?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()
        forgot?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()

        // Button and Footer last
        btnLogin?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(700)?.start()
        signUp?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(800)?.start()
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