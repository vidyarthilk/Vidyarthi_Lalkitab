package com.vidyarthi.lalkitab.ui.kundli

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.utils.KundliHolder

class VarshfalYearFragment : Fragment() {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_varshfal_year, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: return
        VarshfalUi.bind(view, requireContext(), k)
    }
}
