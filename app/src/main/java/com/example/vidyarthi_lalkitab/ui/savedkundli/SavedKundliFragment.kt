package com.example.vidyarthi_lalkitab.ui.savedkundli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vidyarthi_lalkitab.R
import com.example.vidyarthi_lalkitab.data.db.AppDatabase
import kotlinx.coroutines.launch

class SavedKundliFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_saved_kundli, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvSavedKundli)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val list = AppDatabase.getDatabase(requireContext())
                .kundliDao()
                .getAllKundli()

            recyclerView.adapter = SavedKundliAdapter(list)
        }

        return view
    }
}
