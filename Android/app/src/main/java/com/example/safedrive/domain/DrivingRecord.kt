package com.example.safedrive.domain

data class DrivingRecord(
    val year: Int,
    val month: Int,
    val day: Int,
    val start: String,
    val arrive: String,
    val distance: Double,
    val numSudden: Int,
    val numDistance: Int,
    val numSignal: Int
)