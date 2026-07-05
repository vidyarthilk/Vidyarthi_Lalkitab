package com.vidyarthi.lalkitab.ui.panchang

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import com.vidyarthi.lalkitab.ui.theme.Vidyarthi_LalkitabTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PanchangFragment : Fragment() {

    private val panchangVM: PanchangViewModel by viewModels()
    private val sharedKundliVM: SharedKundliViewModel by activityViewModels()

    private var lastLoadedKundliHash: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        observeKundliData()

        return ComposeView(requireContext()).apply {
            setContent {
                Vidyarthi_LalkitabTheme {
                    PanchangScreen(viewModel = panchangVM)
                }
            }
        }
    }

    private fun observeKundliData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {
                sharedKundliVM.kundliData.collectLatest { kundliData ->
                    kundliData ?: return@collectLatest

                    val newHash = kundliData.hashCode()
                    if (lastLoadedKundliHash == newHash) return@collectLatest
                    lastLoadedKundliHash = newHash

                    panchangVM.loadPanchang(kundliData)
                }
            }
        }
    }
}
