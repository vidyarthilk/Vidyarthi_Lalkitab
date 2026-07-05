package com.vidyarthi.lalkitab.data

import java.time.LocalDate

data class PanchangMonthInfo(
    /** 0 = Chaitra … 11 = Phalguna (Amanta). */
    val maasIndex: Int,
    val isAdhik: Boolean,
    val amavasyaDate: LocalDate?
)
