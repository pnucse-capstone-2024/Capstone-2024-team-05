package com.example.safedrive.ui.dashboard

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.safedrive.R
import com.example.safedrive.databinding.FragmentDashboardBinding
import com.example.safedrive.domain.DrivingRecord
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar
import kotlin.math.*

class DashboardFragment : Fragment() {

    private var _fragmentDashboardBinding: FragmentDashboardBinding? = null
    private val fragmentDashboardBinding get() = _fragmentDashboardBinding!!
    private val dashboardViewModel: DashboardViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentDashboardBinding = FragmentDashboardBinding.inflate(inflater, container, false)
        return fragmentDashboardBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupButtonListeners()
        processCameraData()
    }

    private fun setupUI() {
        dashboardViewModel.currentRecord.observe(viewLifecycleOwner) { record ->
            bindRecordToUI(record)
        }
    }

    private fun bindRecordToUI(record: DrivingRecord) {
        fragmentDashboardBinding.apply {

            date.text = "${record.year}년 ${record.month}월 ${record.day}일 기록"
            startLocation.text = record.start
            endLocation.text = record.arrive
            numSudden.text = "${record.numSudden}회"
            numDistance.text = "${record.numDistance}회"
            numSignal.text = "${record.numSignal}회"
            highestDistance.text = "🏅 최고 주행 거리: ${dashboardViewModel.highestDistance.value}km"
            tvDistanceRecord.text = "(${record.distance}km)"

            val calculator = DrivingScoreCalculator()
            val totalScore = calculator.calculateTotalScore(record.numSudden, record.numDistance, record.numSignal)
            score.text = "$totalScore 점"

            driveEmoji.setImageResource(calculator.provideDrivingEmoji(record.numSudden, record.numDistance, record.numSignal))
            warningFeedback.text = calculator.provideDrivingFeedback(record.numSudden, record.numDistance, record.numSignal)

            val scores = calculator.calculateDrivingScores(record.numSudden, record.numDistance, record.numSignal)
            setRadarChart(scores)
        }
    }

    private fun setupButtonListeners() {
        fragmentDashboardBinding.apply {
            nextButton.setOnClickListener { dashboardViewModel.moveToNextRecord() }
            previousButton.setOnClickListener { dashboardViewModel.moveToPreviousRecord() }
        }
    }

    private fun processCameraData() {
        val fromCamera = arguments?.getBoolean("fromCamera") ?: false
        if (fromCamera) {
            val startLatitude = arguments?.getDouble("startLatitude") ?: 0.0
            val startLongitude = arguments?.getDouble("startLongitude") ?: 0.0
            val startAddress = arguments?.getString("startAddress") ?: ""
            val endLatitude = arguments?.getDouble("endLatitude") ?: 0.0
            val endLongitude = arguments?.getDouble("endLongitude") ?: 0.0
            val endAddress = arguments?.getString("endAddress") ?: ""

            val distance = calculateDistanceInKm(startLatitude, startLongitude, endLatitude, endLongitude)

            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val numDistance = arguments?.getInt("numDistance") ?: 0
            val numSignal = arguments?.getInt("numSignal") ?: 0
            val numSudden = arguments?.getInt("numSudden") ?: 0

            val newRecord = DrivingRecord(
                year = year,
                month = month,
                day = day,
                start = startAddress,
                arrive = endAddress,
                distance = distance,
                numSudden = numSudden,
                numDistance = numDistance,
                numSignal = numSignal
            )
            dashboardViewModel.updateDrivingRecord(newRecord)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentDashboardBinding = null
    }

    private fun setRadarChart(scores: List<Float>) {
        fragmentDashboardBinding.radarChart.apply {
            description.isEnabled = false
            webColor = Color.GRAY
            webColorInner = Color.GRAY
            legend.isEnabled = false
            setTouchEnabled(false)

            val labels = arrayOf("운전 안전성", "교통 법규\n준수", "운전 습관", "운전 환경\n대응")
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textColor = Color.GRAY
                xAxis.setTextSize(10f);
                spaceMin = 2f
                spaceMax = 2f
            }

            val entries = scores.map { RadarEntry(it) }
            val dataSet = RadarDataSet(entries, "").apply {
                color = Color.GRAY
                fillColor = ContextCompat.getColor(requireContext(), R.color.blue)
                setDrawFilled(true)
                setDrawValues(false)
                fillAlpha = 180
                lineWidth = 1f
            }

            data = RadarData(dataSet).apply {
                yAxis.axisMaximum = 9f
                yAxis.axisMinimum = 1f
                yAxis.setDrawLabels(false)
            }

            invalidate()
        }
    }

    private fun calculateDistanceInKm(
        startLatitude: Double, startLongitude: Double,
        endLatitude: Double, endLongitude: Double
    ): Double {
        val deltaLatitude = Math.toRadians(endLatitude - startLatitude)
        val deltaLongitude = Math.toRadians(endLongitude - startLongitude)

        val a = sin(deltaLatitude / 2).pow(2.0) +
                sin(deltaLongitude / 2).pow(2.0) *
                cos(Math.toRadians(startLatitude)) *
                cos(Math.toRadians(endLatitude))

        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    companion object {
        const val EARTH_RADIUS_KM = 6372.8
    }
}