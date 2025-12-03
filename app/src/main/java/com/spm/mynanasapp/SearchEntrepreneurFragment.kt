package com.spm.mynanasapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.spm.mynanasapp.data.model.entity.User
import com.spm.mynanasapp.data.model.request.GetUsersRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class SearchEntrepreneurFragment : Fragment() {

    private lateinit var adapter: SearchUserAdapter
    private val allUsers = mutableListOf<User>() // Mock Master List

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search_entrepreneur, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup UI
        val etSearch = view.findViewById<EditText>(R.id.et_search_query)
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_search_results)
        val tvNoResults = view.findViewById<TextView>(R.id.tv_no_results)

        // 2. Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Navigate to individual Profile
        adapter = SearchUserAdapter(emptyList()) { user ->

            // 1. Create Fragment
            val publicProfile = EntrepreneurPublicProfileFragment.newInstance(
                userId = user.entID,
                username = user.ent_username,
                fullname = user.ent_fullname
            )

            // 2. Navigate with SLIDE ANIMATION
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,  // Enter (Slide in from Right)
                    R.anim.slide_out_left,  // Exit (Current screen slides Left)
                    R.anim.slide_in_left,   // Pop Enter (Slide back from Left)
                    R.anim.slide_out_right  // Pop Exit (Slide away to Right)
                )
                .replace(R.id.nav_host_fragment, publicProfile)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        // 3. Load API Data
        loadUsersFromApi()

        // 4. Search Logic (TextWatcher)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                filterUsers(query, tvNoResults)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 5. Back Button
        btnBack.setOnClickListener {
            hideKeyboard()
            parentFragmentManager.popBackStack()
        }

        // 6. Auto-Focus Keyboard
        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun loadUsersFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // specific_user = false to get list
            val request = GetUsersRequest(specific_user = false)

            try {
                // Call the endpoint that returns List<User>
                val response = RetrofitClient.instance.getAllUsers("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    val users = response.body()?.data ?: emptyList()
                    allUsers.clear()
                    allUsers.addAll(users)

                    // Initially show empty or all users? Usually empty until typed.
                    adapter.updateList(emptyList())
                } else {
                    Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterUsers(query: String, emptyView: TextView) {
        if (query.isEmpty()) {
            adapter.updateList(emptyList()) // Show nothing or Recent Searches
            emptyView.visibility = View.GONE
            return
        }

        // Filter Logic
        val filtered = allUsers.filter {
            it.ent_username.lowercase().contains(query) ||
                    it.ent_fullname.lowercase().contains(query)
        }

        adapter.updateList(filtered)

        // Show/Hide Empty State
        if (filtered.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No users found for \"$query\""
        } else {
            emptyView.visibility = View.GONE
        }
    }

    // --- HIDE BOTTOM NAV ---
    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(true)
    }

    private fun hideKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}