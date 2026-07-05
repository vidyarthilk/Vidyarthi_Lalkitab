package com.vidyarthi.lalkitab.ui.lalkitab

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import com.vidyarthi.lalkitab.utils.KundliHolder

class LalKitabMukhyaGrahFlowFragment : Fragment(R.layout.fragment_lalkitab_mukhya_grah) {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val k = sharedVM.kundliData.value ?: KundliHolder.kundliData ?: run {
            Toast.makeText(requireContext(), getString(R.string.toast_kundli_not_available), Toast.LENGTH_SHORT).show()
            return
        }
        LalKitabMukhyaGrahBinder.bind(view, k, viewLifecycleOwner, requireContext())
    }
}
