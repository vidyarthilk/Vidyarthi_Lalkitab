package com.example.vidyarthi_lalkitab.ui.newkundli

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.vidyarthi_lalkitab.R
import com.example.vidyarthi_lalkitab.data.db.AppDatabase
import com.example.vidyarthi_lalkitab.data.entity.KundliEntity
import com.example.vidyarthi_lalkitab.utils.CityUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class NewKundliFragment : Fragment(R.layout.fragment_new_kundli) {

    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var actCity: AutoCompleteTextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        etDate = view.findViewById(R.id.etDate)
        etTime = view.findViewById(R.id.etTime)
        actCity = view.findViewById(R.id.actCity)

        etDate.setOnClickListener { showDatePicker() }
        etTime.setOnClickListener { showTimePicker() }
        setupCityDropdown()

        btnSave.setOnClickListener {

            val name = etName.text.toString()
            val date = etDate.text.toString()
            val time = etTime.text.toString()
            val city = actCity.text.toString()

            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> "પુરુષ"
                R.id.rbFemale -> "સ્ત્રી"
                else -> ""
            }

            if (name.isEmpty() || date.isEmpty() || time.isEmpty() || city.isEmpty() || gender.isEmpty()) {
                Toast.makeText(requireContext(), "બધી વિગતો દાખલ કરો", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kundli = KundliEntity(
                name = name,
                date = date,
                time = time,
                city = city,
                gender = gender
            )

            lifecycleScope.launch {
                AppDatabase.getDatabase(requireContext())
                    .kundliDao()
                    .insertKundli(kundli)

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Kundli Saved", Toast.LENGTH_SHORT).show()
                    etName.text.clear()
                    etDate.text.clear()
                    etTime.text.clear()
                    actCity.text.clear()
                    rgGender.clearCheck()
                }
            }
        }
    }

    private fun setupCityDropdown() {
        val cityList = CityUtils.loadCities(requireContext())
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cityList
        )
        actCity.setAdapter(adapter)
        actCity.threshold = 1
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                etDate.setText("$day/${month + 1}/$year")
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                etTime.setText(String.format("%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }
}
