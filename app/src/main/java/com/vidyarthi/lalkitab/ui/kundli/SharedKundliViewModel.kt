package com.vidyarthi.lalkitab.ui.kundli

import androidx.lifecycle.ViewModel
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.data.entity.KundliEntity
import com.vidyarthi.lalkitab.utils.KundliHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedKundliViewModel : ViewModel() {

    private val _kundliData = MutableStateFlow<KundliData?>(null)
    val kundliData: StateFlow<KundliData?> = _kundliData

    private val _editKundli = MutableStateFlow<KundliEntity?>(null)
    val editKundli: StateFlow<KundliEntity?> = _editKundli

    fun setKundliData(data: KundliData) {
        if (_kundliData.value == data) return
        _kundliData.value = data
    }

    fun openKundliSession(data: KundliData) {
        KundliHolder.kundliData = data
        setKundliData(data)
    }

    fun setEditKundli(kundli: KundliEntity) {
        _editKundli.value = kundli
    }

    fun clearEditKundli() {
        _editKundli.value = null
    }

    fun clearKundliData() {
        _kundliData.value = null
    }
}
