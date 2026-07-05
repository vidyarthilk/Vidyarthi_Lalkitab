package com.vidyarthi.lalkitab.ui.newkundli

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.InputType
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ads.BannerAdHost
import com.vidyarthi.lalkitab.data.CityPick
import com.vidyarthi.lalkitab.data.CityRepository
import com.vidyarthi.lalkitab.data.db.AppDatabase
import com.vidyarthi.lalkitab.data.entity.KundliEntity
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import com.vidyarthi.lalkitab.ui.kundli.KundliFlowLauncher
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import com.vidyarthi.lalkitab.utils.CityUtils
import com.vidyarthi.lalkitab.utils.DateInputFormatter
import com.vidyarthi.lalkitab.utils.KundliDataMapper
import com.vidyarthi.lalkitab.utils.TimeInputFormatter
import com.vidyarthi.lalkitab.utils.WindowInsetsUi
import androidx.core.content.ContextCompat
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class NewKundliFragment : Fragment(R.layout.fragment_new_kundli) {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    private lateinit var etDate: EditText
    private lateinit var etTime: EditText
    private lateinit var actCity: AutoCompleteTextView
    private lateinit var cityRepository: CityRepository
    private lateinit var cityAdapter: CitySearchAdapter
    private var citySearchJob: Job? = null
    private var isSelectingCity = false
    private lateinit var cityTextWatcher: TextWatcher
    private var selectedCityPick: CityPick? = null
    private var editingId: Int? = null
    private val bannerAd = BannerAdHost(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowInsetsUi.applyKeyboardPadding(view.findViewById(R.id.newKundliScroll))
        bannerAd.attach(view)

        val etName = view.findViewById<EditText>(R.id.etName)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        etDate = view.findViewById(R.id.etDate)
        etTime = view.findViewById(R.id.etTime)
        actCity = view.findViewById(R.id.actCity)
        if (!CityUtils.isDatabaseReady(requireContext()) && !CityUtils.copyDatabase(requireContext())) {
            Toast.makeText(requireContext(), R.string.city_db_error, Toast.LENGTH_LONG).show()
            actCity.isEnabled = false
        } else {
            cityRepository = CityRepository(requireContext())
            setupCityDropdown()
        }

        DateInputFormatter.attach(etDate)
        etDate.setOnClickListener { showDatePicker() }
        etTime.setOnClickListener { showTimePicker() }
        if (::cityRepository.isInitialized) {
            observeEditKundli(etName, rgGender)
        }

        btnSave.setOnClickListener {
            val ctx = requireContext()
            if (!::cityRepository.isInitialized) {
                Toast.makeText(ctx, R.string.city_db_error, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val name = etName.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()
            val cityLine = actCity.text.toString().trim()

            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMale -> ctx.getString(R.string.gender_male)
                R.id.rbFemale -> ctx.getString(R.string.gender_female)
                else -> ""
            }

            if (name.isEmpty() || date.isEmpty() || time.isEmpty() || cityLine.isEmpty() || gender.isEmpty()) {
                Toast.makeText(ctx, ctx.getString(R.string.toast_fill_all), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val resolved = withContext(Dispatchers.IO) {
                    val sel = selectedCityPick
                    if (sel != null && sel.toString() == cityLine) sel
                    else cityRepository.resolveCityPickForKundli(cityLine)
                }
                if (resolved == null) {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.toast_select_city),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val cityToSave = resolved.toString()
                val kundliData = KundliDataMapper.fromCityPick(name, date, time, resolved)
                if (kundliData == null) {
                    Toast.makeText(
                        requireContext(),
                        requireContext().getString(R.string.toast_error_prefix, "date/time"),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val existingId = editingId
                val kundli = if (existingId != null) {
                    KundliEntity(
                        id = existingId,
                        name = name,
                        date = date,
                        time = time,
                        city = cityToSave,
                        gender = gender
                    )
                } else {
                    KundliEntity(
                        name = name,
                        date = date,
                        time = time,
                        city = cityToSave,
                        gender = gender
                    )
                }

                val appCtx = requireContext().applicationContext
                val savedOk = withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getDatabase(appCtx).kundliDao()
                    if (existingId != null) {
                        dao.updateKundli(kundli)
                        true
                    } else {
                        val count = dao.countKundli()
                        if (!SubscriptionManager.canSaveAnother(appCtx, count, false)) {
                            false
                        } else {
                            dao.insertKundli(kundli)
                            SubscriptionManager.recordLifetimeSave(appCtx)
                            true
                        }
                    }
                }
                if (!savedOk) {
                    Toast.makeText(
                        requireContext(),
                        SubscriptionManager.limitReachedMessage(appCtx),
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val wasUpdate = existingId != null
                editingId = null

                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        if (wasUpdate) {
                            requireContext().getString(R.string.toast_kundli_updated)
                        } else {
                            requireContext().getString(R.string.toast_kundli_saved)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    clearForm(etName, rgGender)
                    KundliFlowLauncher.open(
                        requireContext(),
                        kundliData,
                        R.id.nav_panchang,
                        city = cityToSave,
                        gender = gender
                    )
                }
            }
        }
    }

    private fun observeEditKundli(etName: EditText, rgGender: RadioGroup) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedVM.editKundli.collect { entity ->
                    entity ?: return@collect
                    fillFormForEdit(entity, etName, rgGender)
                    sharedVM.clearEditKundli()
                }
            }
        }
    }

    private fun fillFormForEdit(entity: KundliEntity, etName: EditText, rgGender: RadioGroup) {
        editingId = entity.id
        etName.setText(entity.name)
        etDate.setText(entity.date)
        etTime.setText(TimeInputFormatter.toDisplayTime(entity.time))
        actCity.removeTextChangedListener(cityTextWatcher)
        actCity.setText(entity.city)
        actCity.addTextChangedListener(cityTextWatcher)
        val ctx = requireContext()
        when (entity.gender) {
            ctx.getString(R.string.gender_male) -> rgGender.check(R.id.rbMale)
            ctx.getString(R.string.gender_female) -> rgGender.check(R.id.rbFemale)
        }
        lifecycleScope.launch {
            selectedCityPick = withContext(Dispatchers.IO) {
                cityRepository.resolveCityPickForKundli(entity.city)
            }
        }
    }

    private fun clearForm(etName: EditText, rgGender: RadioGroup) {
        editingId = null
        etName.text.clear()
        etDate.text.clear()
        etTime.text.clear()
        actCity.text.clear()
        rgGender.clearCheck()
        selectedCityPick = null
        cityAdapter.clearItems()
    }

    private fun setupCityDropdown() {
        cityAdapter = CitySearchAdapter(requireContext())
        actCity.setAdapter(cityAdapter)
        actCity.threshold = 2
        actCity.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_CAP_WORDS
        actCity.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        ContextCompat.getDrawable(requireContext(), R.drawable.bg_city_dropdown)?.let {
            actCity.setDropDownBackgroundDrawable(it)
        }

        cityTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isSelectingCity) {
                    isSelectingCity = false
                    return
                }
                selectedCityPick = null
                val q = s?.toString()?.trim().orEmpty()
                citySearchJob?.cancel()
                if (q.length < 2) {
                    cityAdapter.clearItems()
                    actCity.dismissDropDown()
                    return
                }
                citySearchJob = lifecycleScope.launch {
                    delay(250)
                    if (!isActive) return@launch
                    val list = withContext(Dispatchers.IO) {
                        cityRepository.searchCityPicks(q)
                    }
                    cityAdapter.replaceAll(list)
                    if (list.isNotEmpty() && actCity.hasFocus() && isActive) {
                        actCity.showDropDown()
                    }
                }
            }
        }
        actCity.addTextChangedListener(cityTextWatcher)

        actCity.setOnItemClickListener { parent, _, position, _ ->
            val pick = parent.getItemAtPosition(position) as? CityPick ?: return@setOnItemClickListener
            isSelectingCity = true
            selectedCityPick = pick
            actCity.removeTextChangedListener(cityTextWatcher)
            actCity.setText(pick.toString())
            actCity.setSelection(actCity.text.length)
            actCity.dismissDropDown()
            actCity.addTextChangedListener(cityTextWatcher)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            R.style.Theme_Vidyarthi_Dialog,
            { _, year, month, day ->
                etDate.setText(DateInputFormatter.formatFromPicker(day, month + 1, year))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val parsed = TimeInputFormatter.parseTo24Hour(etTime.text.toString().trim())
        val cal = Calendar.getInstance()
        val hour = parsed?.first ?: cal.get(Calendar.HOUR_OF_DAY)
        val minute = parsed?.second ?: cal.get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            .setTheme(R.style.ThemeOverlay_Vidyarthi_TimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            etTime.setText(TimeInputFormatter.formatFromPicker(picker.hour, picker.minute))
        }
        picker.show(parentFragmentManager, "birth_time_picker")
    }

    override fun onResume() {
        super.onResume()
        bannerAd.onResume()
    }

    override fun onPause() {
        bannerAd.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        bannerAd.onDestroyView()
        super.onDestroyView()
    }
}
