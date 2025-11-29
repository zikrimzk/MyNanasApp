package com.spm.mynanasapp

import android.location.Geocoder
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationPickerBottomSheet(
    private val onLocationSelected: (String?) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearch = view.findViewById<EditText>(R.id.et_search_location)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_search)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_search_results)
        val btnCurrent = view.findViewById<LinearLayout>(R.id.item_current_location)

        // 1. Handle "Use Current Location" Click
        btnCurrent.setOnClickListener {
            onLocationSelected(null)
            dismiss()
        }

        // 2. Setup Search Input Listener
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {

                val query = etSearch.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query, progressBar, recyclerView)
                }
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String, progressBar: ProgressBar, recyclerView: RecyclerView) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 10)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (!results.isNullOrEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = SearchResultsAdapter(results) { selectedName ->
                            onLocationSelected(selectedName)
                            dismiss()
                        }
                    } else {
                        Toast.makeText(context, "No city found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Search failed. Check internet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class SearchResultsAdapter(
        private val addresses: List<android.location.Address>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val address = addresses[position]
            val sb = StringBuilder()
            if (address.featureName != null) sb.append(address.featureName).append(", ")
            if (address.locality != null) sb.append(address.locality).append(", ")
            if (address.adminArea != null) sb.append(address.adminArea)

            val locationName = sb.toString()
            holder.tvName.text = locationName

            holder.itemView.setOnClickListener {
                onClick(locationName)
            }
        }

        override fun getItemCount() = addresses.size
    }
}