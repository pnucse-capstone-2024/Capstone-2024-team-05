package com.example.safedrive.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.safedrive.domain.DrivingRecord

class DashboardViewModel : ViewModel() {

    private val _currentRecord = MutableLiveData<DrivingRecord>()
    val currentRecord: LiveData<DrivingRecord> = _currentRecord

    private val _records = mutableListOf<DrivingRecord>()
    val records: List<DrivingRecord> get() = _records

    private var currentIndex = 0

    private val _highestDistance = MutableLiveData<Double>()
    val highestDistance: LiveData<Double> get() = _highestDistance

    init {
        _records.add(DrivingRecord( 2023, 9, 20, "대전역", "부산역", 12.3,0, 1, 0))
        _records.add(DrivingRecord( 2024, 10, 1, "부산대", "광안리 해수욕장", 19.2, 1, 2, 1))
        _records.add(DrivingRecord(2024, 10, 5, "서울역", "인천국제공항", 62.0,2, 1, 3))

        currentIndex = records.size - 1
        _currentRecord.value = records[currentIndex]

        calculateHighestDistance()
    }

    fun moveToNextRecord() {
        if (currentIndex < records.size - 1) {
            currentIndex++
            _currentRecord.value = records[currentIndex]
        }
    }

    fun moveToPreviousRecord() {
        if (currentIndex > 0) {
            currentIndex--
            _currentRecord.value = records[currentIndex]
        }
    }

    fun updateDrivingRecord(newRecord: DrivingRecord) {
        _records.add(newRecord)
        currentIndex++
        _currentRecord.value = newRecord
        calculateHighestDistance()
    }

    private fun calculateHighestDistance() {
        val maxDistance = _records.maxByOrNull { it.distance }?.distance ?: 0.0
        _highestDistance.value = maxDistance
    }

}

