package com.vidyarthi.lalkitab.navigation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import kotlinx.coroutines.launch

abstract class KundliContentShellFragment : Fragment(R.layout.fragment_kundli_content_shell) {

    protected val sharedVM: SharedKundliViewModel by activityViewModels()

    private var contentAttached = false
    private val contentTag = "kundli_content_child"

    protected abstract fun createContentFragment(): Fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentAttached = childFragmentManager.findFragmentByTag(contentTag) != null
        observeShellState()
    }

    private fun observeShellState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVM.kundliData.collect { data ->
                    renderShell(hasKundli = data != null)
                }
            }
        }
    }

    private fun renderShell(hasKundli: Boolean) {
        val root = view ?: return
        val placeholder = root.findViewById<View>(R.id.panelKundliPlaceholder)
        val container = root.findViewById<View>(R.id.contentContainer)

        if (!hasKundli) {
            placeholder.visibility = View.VISIBLE
            container.visibility = View.GONE
            detachContent()
        } else {
            placeholder.visibility = View.GONE
            container.visibility = View.VISIBLE
            attachContentIfNeeded()
        }
    }

    private fun attachContentIfNeeded() {
        if (childFragmentManager.findFragmentByTag(contentTag) != null) {
            contentAttached = true
            return
        }
        if (contentAttached) return
        contentAttached = true
        childFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, createContentFragment(), contentTag)
            .commitNowAllowingStateLoss()
    }

    private fun detachContent() {
        if (!contentAttached) return
        contentAttached = false
        childFragmentManager.findFragmentByTag(contentTag)?.let { child ->
            childFragmentManager.beginTransaction().remove(child).commitNowAllowingStateLoss()
        }
    }
}
