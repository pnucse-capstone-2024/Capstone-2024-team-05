package com.example.safedrive.ui.map

import ViolationData
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safedrive.BuildConfig
import com.example.safedrive.R
import com.example.safedrive.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapTapi
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import com.skt.tmap.poi.TMapPOIItem
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class MapFragment : Fragment() {

    private val TAG = "MapFragment"

    private var _fragmentMapBinding: FragmentMapBinding? = null
    private val fragmentMapBinding get() = _fragmentMapBinding!!

    private var violationDataList: List<ViolationData> = emptyList()
    private lateinit var poiAdapter: POIAdapter
    private lateinit var tMapView: TMapView
    private lateinit var tMapData: TMapData
    private lateinit var tMapTapi: TMapTapi
    private var currentPolyLine: TMapPolyLine? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val PERMISSION_REQUEST_CODE = 100
    private var startLatitude: Double = 37.570841
    private var startLongitude: Double = 126.985302
    private lateinit var startPoint: TMapPoint

    private val retryHandler = android.os.Handler()
    private var retryCount = 0
    private val maxRetryCount = 5 // 최대 재시도 횟수
    private var retryDelay = 5000L // 초기 재시도 간격 1초 (1000밀리초)

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentMapBinding = FragmentMapBinding.inflate(inflater, container, false)
        tMapView = TMapView(requireContext())
        tMapTapi = TMapTapi(requireContext())
        tMapData = TMapData()


        // EditText X 아이콘 클릭 시
        fragmentMapBinding.etSearchStart.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (fragmentMapBinding.etSearchStart.right - fragmentMapBinding.etSearchStart.compoundDrawables[2].bounds.width())) {
                    fragmentMapBinding.etSearchStart.text.clear()
                    fragmentMapBinding.poiRecyclerView.visibility = View.GONE
                    return@setOnTouchListener true
                }
            }
            false
        }

        // 현재 위치를 지도에 초점
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        getCurrentLocation()

        // 지도 초기화
        initMapView()

        // 장소 목록 클릭 시
        setupSearchListener()

        return fragmentMapBinding.root
    }

    private fun setDestination(poiItem: TMapPOIItem) {
        fragmentMapBinding.etSearchStart.setText(poiItem.poiName)
        val startPoint = TMapPoint(startLatitude, startLongitude)
        val destinationPoint = poiItem.poiPoint

        currentPolyLine?.let {
            tMapView.removeTMapPolyLine(it.id)
            tMapView.removeTMapMarkerItem("destination")
        }

        // 도착지 정보 표시
        fragmentMapBinding.tvPoiName.text = poiItem.poiName.toString()
        fragmentMapBinding.tvPoiAddress.text = "${poiItem.upperAddrName} ${poiItem.middleAddrName} ${poiItem.lowerAddrName} ${poiItem.detailAddrName}"
        fragmentMapBinding.tvPoiTelNo.text = poiItem.telNo.toString()
        fragmentMapBinding.placeInfoBottomSheet.visibility = View.VISIBLE


        // 경로 시작 클릭 시
        fragmentMapBinding.btnNavigation.setOnClickListener {
            activity?.let { context ->
                val tMapTapi = TMapTapi(context)
                val isInstalled = tMapTapi.isTmapApplicationInstalled

                if (isInstalled) {
                    // TMap 길 안내 호출
                    tMapTapi.invokeNavigate(
                        poiItem.poiName,
                        poiItem.poiPoint.longitude.toFloat(),
                        poiItem.poiPoint.latitude.toFloat(),
                        0,
                        true // 도착 시 T Map 자동 종료
                    )
                }
                else {
                    Toast.makeText(context, "TMap이 설치되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 도착지 마커 표시
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_marker)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)
        val marker = TMapMarkerItem().apply {
            id = "destination"
            setTMapPoint(poiItem.poiPoint)
            icon = resizedBitmap
        }
        tMapView.addTMapMarkerItem(marker)

        // 경로 표시
        TMapData().findPathData(startPoint, destinationPoint, object : TMapData.OnFindPathDataListener {
            override fun onFindPathData(tMapPolyLine: TMapPolyLine?) {
                tMapPolyLine?.let { originalPath ->
                    originalPath.setID("originalPath")
                    originalPath.lineColor = Color.BLUE
                    originalPath.lineWidth = 1f
                    currentPolyLine = originalPath
                    val info = tMapView.getDisplayTMapInfo(originalPath.linePointList)
                    tMapView.zoomLevel = info.getZoom()
                    tMapView.setCenterPoint(info.getPoint().getLatitude(), info.getPoint().getLongitude())
                    tMapView.addTMapPolyLine(originalPath)

                } ?: Log.e(TAG, "경로 데이터를 찾을 수 없습니다.")
            }
        })

        fragmentMapBinding.poiRecyclerView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentMapBinding = null
    }

    private fun initMapView() {
        val tMapUILayout: FrameLayout = fragmentMapBinding.tMapUILayout
        fragmentMapBinding.poiRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        poiAdapter = POIAdapter(emptyList()) { poiItem ->
            setDestination(poiItem)
        }
        fragmentMapBinding.poiRecyclerView.adapter = poiAdapter

        // API 인증 시도
        tMapView.setSKTMapApiKey(BuildConfig.API_KEY)
        tMapUILayout.addView(tMapView)

        // API 인증 성공/실패 처리
        tMapView.setOnApiKeyListenerCallback(object : TMapView.OnApiKeyListenerCallback {
            override fun onSKTMapApikeySucceed() {
                Log.d(TAG, "TMap API 인증 성공")

                // 재시도 중지
                retryHandler.removeCallbacks(retryRunnable)

                // API 인증 성공 시 데이터 로드 및 작업 수행
                violationDataList = readCSVFromAssets("17_23_violation.csv")
                addViolationPolylines()
            }

            override fun onSKTMapApikeyFailed(p0: String?) {
                Log.e(TAG, "TMap API 인증 실패. 재시도 시작.")
                retryCount = 0 // 재시도 카운트 초기화
                retryDelay = 5000L // 재시도 간격 초기화
                retryHandler.post(retryRunnable) // 재시도 시작
            }
        })
    }

    private fun addViolationPolylines() {
        val dataProcessor = AccidentDataProcessor()

        tMapView.setCenterPoint(startPoint.latitude, startPoint.longitude)
        violationDataList.forEach { data ->
            val polylinePoints: ArrayList<TMapPoint> = parseCoordinatesFromJson(data.geomJson)
            val violationPolyLine = TMapPolyLine("polyline_${data.spotName}_${System.currentTimeMillis()}", polylinePoints)
            val severityScore = dataProcessor.calculateSeverityScore(data)
            val severityColor = dataProcessor.getSeverityColor(severityScore)
            violationPolyLine.lineColor = severityColor
            violationPolyLine.outLineColor = severityColor
            tMapView.addTMapPolyLine(violationPolyLine)
        }
    }

    // T Map API 인증을 재시도하는 Runnable
    private val retryRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "TMap API 인증 재시도 중... (시도 횟수: $retryCount)")

            tMapView.setSKTMapApiKey(BuildConfig.API_KEY) // 다시 API 인증 시도

            retryCount++

            if (retryCount < maxRetryCount) {
                retryHandler.postDelayed(this, retryDelay)
                retryDelay *= 2 // 백오프: 재시도할 때마다 지연 시간 2배로 증가
            } else {
                Log.e(TAG, "TMap API 인증 실패. 최대 재시도 횟수를 초과했습니다.")
            }
        }
    }

    private fun readCSVFromAssets(fileName: String): List<ViolationData> {
        val violationDataList = mutableListOf<ViolationData>()
        try {
            val reader = BufferedReader(InputStreamReader(requireContext().assets.open(fileName)))
            reader.readLine() // 첫 줄(헤더) 건너뛰기

            reader.forEachLine { line ->
                val row = line.split(",").toMutableList()
                if (row.size >= 16) {
                    val geomJson = row.subList(15, row.size).joinToString(",")
                    violationDataList.add(
                        ViolationData(
                            afosFid = row[0].toLongOrNull() ?: 0L,
                            afosId = row[1].toLongOrNull() ?: 0L,
                            violationType = row[2],
                            bjdCode = row[3],
                            spotCode = row[4],
                            sidoSggName = row[5],
                            spotName = row[6],
                            occrrncCnt = row[7].toIntOrNull() ?: 0,
                            casltCnt = row[8].toIntOrNull() ?: 0,
                            dthDnvCnt = row[9].toIntOrNull() ?: 0,
                            seDnvCnt = row[10].toIntOrNull() ?: 0,
                            slDnvCnt = row[11].toIntOrNull() ?: 0,
                            wndDnvCnt = row[12].toIntOrNull() ?: 0,
                            loCrd = row[13].toDoubleOrNull() ?: 0.0,
                            laCrd = row[14].toDoubleOrNull() ?: 0.0,
                            geomJson = geomJson
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CSV_READER", "Error reading file: ${e.message}")
        }
        return violationDataList
    }

    private fun parseCoordinatesFromJson(geomJson: String): ArrayList<TMapPoint> {
        val pointList = ArrayList<TMapPoint>()

        try {
            var cleanedJson = geomJson.replace("\"\"", "\"")
            if (cleanedJson.startsWith("\"") && cleanedJson.endsWith("\"")) {
                cleanedJson = cleanedJson.substring(1, cleanedJson.length - 1)
            }

            val coordinatesArray = JSONObject(cleanedJson)
                .getJSONArray("coordinates").getJSONArray(0)

            for (i in 0 until coordinatesArray.length()) {
                val pointArray = coordinatesArray.getJSONArray(i)
                val lon = pointArray.getDouble(0)
                val lat = pointArray.getDouble(1)
                pointList.add(TMapPoint(lat, lon))
            }
        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "Error parsing geomJson: ${e.message}")
        }

        return pointList
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(listener: OnTokenCanceledListener) = CancellationTokenSource().token
                override fun isCancellationRequested() = false
            }
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                startLatitude = location.latitude
                startLongitude = location.longitude
                startPoint = TMapPoint(startLatitude, startLongitude)
                Toast.makeText(requireContext(), "위도: $startLatitude, 경도: $startLongitude", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchListener() {
        fragmentMapBinding.etSearchStart.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (it.isNotBlank()) {
                        fragmentMapBinding.poiRecyclerView.visibility = View.VISIBLE
                        searchLocation(it.toString())
                    }
                    else {
                        fragmentMapBinding.poiRecyclerView.visibility = View.GONE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 위치 검색
    private fun searchLocation(query: String) {
        tMapData.findAllPOI(query, object : TMapData.OnFindAllPOIListener {
            override fun onFindAllPOI(items: ArrayList<TMapPOIItem>?) {
                activity?.runOnUiThread {
                    items?.let {
                        poiAdapter.updatePOIList(it)
                        Log.d(TAG, "POI 검색 결과: ${items.size} 개")
                    } ?: Log.d(TAG, "검색 결과가 없습니다.")
                }
            }
        })
    }
}