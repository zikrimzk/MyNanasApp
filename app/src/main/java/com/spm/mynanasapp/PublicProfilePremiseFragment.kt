package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.request.GetPremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class PublicProfilePremiseFragment : Fragment() {

    private var userId: Long = 0
    private lateinit var adapter: ProfilePremiseAdapter // Modified adapter needed (hide Edit/Delete)
    private val premiseList = mutableListOf<Premise>()

    companion object {
        fun newInstance(userId: Long) = PublicProfilePremiseFragment().apply {
            arguments = Bundle().apply { putLong("USER_ID", userId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong("USER_ID") ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_public_profile_premise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Setup Recycler
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_premises)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // We need an adapter that DOESN'T show the edit/delete menu
        // Ideally pass "isReadOnly = true" to the adapter
        adapter = ProfilePremiseAdapter(premiseList,
            onEdit = {},
            onDelete = {},
            isReadOnly = true
        )
        recyclerView.adapter = adapter

        // 3. Load Data
        loadPremisesFromApi()
    }

    private fun loadPremisesFromApi() {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)

        // Show Progress Bar only if NOT pulling to refresh
        if (swipeRefresh?.isRefreshing == false) {
            progressBar?.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Get All premises for specific user
            val request = GetPremiseRequest(premise_type = "All", specific_user = true, entID = userId)

            try {
                val response = RetrofitClient.instance.getPremises("Bearer $token", request)
                if (response.isSuccessful && response.body()?.status == true) {
                    val data = response.body()?.data ?: emptyList()
                    premiseList.clear()
                    premiseList.addAll(data)
                    adapter.notifyDataSetChanged()
                    checkEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load premises", Toast.LENGTH_SHORT).show()
            } finally {
                // Stop animations
                progressBar?.visibility = View.GONE
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun checkEmptyState() {
        if (!isAdded || view == null) return
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_premises)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_view)

        if (premiseList.isEmpty()) {
            // === STATE: EMPTY ===
            recyclerView?.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE

            // Customize Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)
            val btnPlaceholder = emptyLayout?.findViewById<MaterialButton>(R.id.btn_tab_action)

            tvTitle?.text = "No Premises Registered"
            tvDesc?.text = "There are no premise registered by this user."
            ivIcon?.setImageResource(R.drawable.ic_tab_farm)

        } else {
            // === STATE: HAS DATA ===
            emptyLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }
}