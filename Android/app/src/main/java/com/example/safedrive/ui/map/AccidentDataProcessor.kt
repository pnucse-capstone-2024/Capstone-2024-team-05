package com.example.safedrive.ui.map

import ViolationData
import android.graphics.Color

class AccidentDataProcessor {

    fun calculateSeverityScore(violationData: ViolationData): Double {
        val occrrncCnt = violationData.occrrncCnt
        val casltCnt = violationData.casltCnt
        val dthDnvCnt = violationData.dthDnvCnt
        val seDnvCnt = violationData.seDnvCnt
        val slDnvCnt = violationData.slDnvCnt
        val wndDnvCnt = violationData.wndDnvCnt

        return occrrncCnt * 0.4 + casltCnt * 0.2 + dthDnvCnt * 0.2 + seDnvCnt * 0.1 + slDnvCnt * 0.05 + wndDnvCnt * 0.05
    }

    fun getSeverityColor(severityScore: Double): Int {
        return when {
            severityScore > 7 -> Color.RED
            severityScore > 3 -> Color.parseColor("#FFA500") // ORANGE
            else -> Color.YELLOW
        }
    }
}