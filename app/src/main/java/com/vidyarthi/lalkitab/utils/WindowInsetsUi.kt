package com.vidyarthi.lalkitab.utils

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/** Keeps content below status bar / above navigation bar on all screen sizes. */
object WindowInsetsUi {

    fun applyToContent(target: View?) {
        target ?: return
        val initialPadding = Insets.of(
            target.paddingLeft,
            target.paddingTop,
            target.paddingRight,
            target.paddingBottom
        )
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                initialPadding.left + bars.left,
                initialPadding.top + bars.top,
                initialPadding.right + bars.right,
                initialPadding.bottom + bars.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(target)
    }

    /** Extra bottom padding when keyboard is open (e.g. New Kundli form). */
    fun applyKeyboardPadding(target: View?) {
        target ?: return
        val initialPadding = Insets.of(
            target.paddingLeft,
            target.paddingTop,
            target.paddingRight,
            target.paddingBottom
        )
        ViewCompat.setOnApplyWindowInsetsListener(target) { view, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraBottom = (ime.bottom - bars.bottom).coerceAtLeast(0)
            view.setPadding(
                initialPadding.left,
                initialPadding.top,
                initialPadding.right,
                initialPadding.bottom + extraBottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(target)
    }
}
