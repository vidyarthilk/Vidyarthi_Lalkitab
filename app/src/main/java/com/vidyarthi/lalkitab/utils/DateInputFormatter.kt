package com.vidyarthi.lalkitab.utils

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText

/** Formats typed digits as dd/mm/yyyy with automatic / separators. */
object DateInputFormatter {

    fun attach(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.addTextChangedListener(object : TextWatcher {
            private var selfChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (selfChange || s == null) return
                val digits = s.toString().filter { it.isDigit() }.take(8)
                val formatted = formatDigits(digits)
                if (formatted == s.toString()) return
                selfChange = true
                editText.setText(formatted)
                editText.setSelection(formatted.length.coerceIn(0, formatted.length))
                selfChange = false
            }
        })
    }

    fun formatDigits(digits: String): String {
        val d = digits.filter { it.isDigit() }.take(8)
        if (d.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in d.indices) {
            if (i == 2 || i == 4) sb.append('/')
            sb.append(d[i])
        }
        return sb.toString()
    }

    fun formatFromPicker(day: Int, month: Int, year: Int): String =
        String.format("%02d/%02d/%04d", day, month, year)
}
