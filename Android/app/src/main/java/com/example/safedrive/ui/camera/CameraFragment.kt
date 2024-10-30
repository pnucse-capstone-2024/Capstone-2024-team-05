package com.example.safedrive.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safedrive.BuildConfig
import com.example.safedrive.MainActivity
import com.example.safedrive.databinding.FragmentCameraBinding
import com.example.safedrive.ui.camera.Constants.LABELS_PATH
import com.example.safedrive.ui.camera.Constants.MODEL_PATH
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.safedrive.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.skt.tmap.TMapData
import com.skt.tmap.TMapTapi
import java.util.Locale

class CameraFragment : Fragment(), Detector.DetectorListener {
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var detector: Detector
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var laneDetectionHelper: LaneDetectionHelper
    private lateinit var rotatedBitmap: Bitmap
    private var tts: TextToSpeech? = null  // TextToSpeech 멤버 변수로 선언

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val PERMISSION_REQUEST_CODE = 100
    private var startLatitude: Double? = null
    private var startLongitude: Double? = null
    private var startAddress: String? = null
    private var endLatitude: Double? = null
    private var endLongitude: Double? = null
    private var endAddress: String? = null
    private var numSudden: Int = 0
    private var numDistance: Int = 0
    private var numSignal: Int = 0

    private var isAnalyzing = false // 모델 감지 활성화 여부를 관리
    private var previousY: Float? = null  // 이전 위치를 저장할 변수

    private var isDisplayingSafetyMessage: Boolean = false // tts flag

    private var previousTrafficLightState: String? = null // signal detection
    private var isVehicleMoving: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        showEmergencyPopup()

        // 지수 추가 부분 ----------------------------
        System.loadLibrary("opencv_java4")

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded.")
        }

        laneDetectionHelper = LaneDetectionHelper()  // 차선 인식 도우미 인스턴스 생성

        detector = Detector(requireContext(), MODEL_PATH, LABELS_PATH, this)
        detector.setup()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mainActivity = activity as MainActivity
        val tts = mainActivity.getTextToSpeech()
        val tMapTapi: TMapTapi = TMapTapi(requireContext())
        val tMapData: TMapData = TMapData()

        setupDriveButton()
        setupTextToSpeech(tts)

        fragmentCameraBinding.driveButton.setOnClickListener {
            if (fragmentCameraBinding.driveButton.text == "주행 시작") {
                handleDriveStart(tMapTapi, tMapData)

                isAnalyzing = true  // 모델 감지 시작
                fragmentCameraBinding.driveButton.text = "주행 종료" // 버튼 텍스트 변경
            }
            else if (fragmentCameraBinding.driveButton.text == "주행 종료") {
                handleDriveEnd(tMapTapi, tMapData)

                isAnalyzing = false  // 모델 감지 중단
                fragmentCameraBinding.driveButton.text = "주행 시작" // 버튼 텍스트 변경
                releaseCameraResources()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = fragmentCameraBinding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            if (isAnalyzing) {  // 주행 시작 시에만 모델 감지를 수행
                val bitmapBuffer =
                    Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
                imageProxy.close()

                val matrix = Matrix().apply {
                    postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                }

                rotatedBitmap = Bitmap.createBitmap(
                    bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true
                )

                // YOLO 모델을 사용한 객체 감지
                val boundingBoxes = detector.detect(rotatedBitmap)

                // 차선 인식 및 YOLO 객체 탐지 결과를 OverlayView에 함께 그리기
                if (boundingBoxes != null) {
                    activity?.runOnUiThread {
                        val laneBitmap = generateLaneBitmap()
                        fragmentCameraBinding.overlay.setResults(boundingBoxes, laneBitmap)
                        fragmentCameraBinding.overlay.invalidate()  // 다시 그리기
                    }
                }
            } else {
                imageProxy.close() // 모델 감지가 비활성화된 경우에는 바로 닫음
            }
        }

        try {
            camera = cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e("CameraFragment", "Use case binding failed", exc)
        }
    }

    // 차선 인식 결과를 생성하는 함수
    private fun generateLaneBitmap(): Bitmap {
        // YOLO 탐지 이후의 차선 인식 로직을 사용하여 처리된 Bitmap 반환
        val edges = laneDetectionHelper.detectEdges(rotatedBitmap)
        val roi = laneDetectionHelper.regionOfInterest(edges)
        val lanes = laneDetectionHelper.detectLanes(roi)
        return laneDetectionHelper.drawLaneLines(rotatedBitmap, lanes)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    private fun setupTextToSpeech(tts: TextToSpeech?) {
        val ttsText = fragmentCameraBinding.bottomSheetLayout.navigationMessage
        tts?.speak("안저니와 안전운전! 주행 시작 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null)
        ttsText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("TextWatcher", "Text changed to: $s")
            }

            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    Log.d("TextWatcher", "Final text is: ${s.toString()}")
                    if (!tts?.isSpeaking()!!) {
                        tts?.speak(s.toString(), TextToSpeech.QUEUE_ADD, null, null)
                    }
                }
            }
        })
    }

    private fun setupDriveButton() {
        fragmentCameraBinding.driveButton.apply {
            text = "주행 시작"
        }
    }

    private fun handleDriveStart(tMapTapi: TMapTapi, tMapData: TMapData) {
        fragmentCameraBinding.driveButton.text = "주행 종료"
        getCurrentLocation { latitude, longitude ->
            startLatitude = latitude
            startLongitude = longitude
            startAddress = getAddressFromLocation(latitude, longitude).toString()

            tMapTapi.setSKTmapAuthentication(BuildConfig.API_KEY)
            tMapTapi.setOnAuthenticationListenerCallback(object : TMapTapi.OnAuthenticationListenerCallback {
                override fun SKTMapApikeySucceed() {
                    Log.d("cameraFragment", "TMap API 인증 성공")
                    fetchAddress(tMapData, latitude, longitude) { address ->
                        if (address != null) {
                            startAddress = address
                            Log.d("cameraFragment", "시작 주소: $startAddress")
                        }
                    }
                }

                override fun SKTMapApikeyFailed(errorMsg: String?) {
                    Log.e("cameraFragment", "TMap API 인증 실패: $errorMsg")
                }
            })
        }
    }

    private fun handleDriveEnd(tMapTapi: TMapTapi, tMapData: TMapData) {
        fragmentCameraBinding.driveButton.text = "주행 시작"
        getCurrentLocation { latitude, longitude ->
            endLatitude = latitude
            endLongitude = longitude
            endAddress = getAddressFromLocation(latitude, longitude).toString()

            tMapTapi.setSKTmapAuthentication(BuildConfig.API_KEY)
            tMapTapi.setOnAuthenticationListenerCallback(object : TMapTapi.OnAuthenticationListenerCallback {
                override fun SKTMapApikeySucceed() {
                    Log.d("cameraFragment", "TMap API 인증 성공")
                    fetchAddress(tMapData, latitude, longitude) { address ->
                        if (address != null) {
                            endAddress = address
                            Log.d("cameraFragment", "도착 주소: $endAddress")
                        }
                        navigateToDashboard()
                    }
                }

                override fun SKTMapApikeyFailed(errorMsg: String?) {
                    Log.e("cameraFragment", "TMap API 인증 실패: $errorMsg")
                }
            })
        }
    }

    private fun fetchAddress(tMapData: TMapData, latitude: Double, longitude: Double, callback: (String?) -> Unit) {
        tMapData.convertGpsToAddress(latitude, longitude, object : TMapData.OnConvertGPSToAddressListener {
            override fun onConverGPSToAddress(address: String?) {
                requireActivity().runOnUiThread {
                    callback(address)
                }
            }
        })
    }

    private fun navigateToDashboard() {
        val bundle = Bundle().apply {
            putDouble("startLatitude", startLatitude ?: 0.0)
            putDouble("startLongitude", startLongitude ?: 0.0)
            putString("startAddress", startAddress ?: "")
            putString("endAddress", endAddress ?: "")
            putDouble("endLatitude", endLatitude ?: 0.0)
            putDouble("endLongitude", endLongitude ?: 0.0)
            putInt("numSudden", numSudden)
            putInt("numSignal", numSignal)
            putInt("numDistance", numDistance)
            putBoolean("fromCamera", true)
        }
        findNavController().navigate(R.id.action_camera_to_dashboard, bundle)
    }

    private fun getCurrentLocation(callback: (Double, Double) -> Unit) {
        if (!isAdded()) {
            Log.e("MapFragment", "Fragment not attached to a context.")
            return
        }

        val context = requireContext()  // 프래그먼트가 연결된 경우에만 실행
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
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

        fusedLocationProviderClient.getCurrentLocation(
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(listener: OnTokenCanceledListener) = CancellationTokenSource().token
                override fun isCancellationRequested() = false
            }
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                callback(latitude, longitude)
            } else {
                Toast.makeText(requireContext(), "위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "위치 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun getAddressFromLocation(latitude: Double?, longitude: Double?): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                address.adminArea
            }
            else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun onPause() {
        super.onPause()
        releaseCameraResources()
    }

    override fun onStop() {
        super.onStop()
        releaseCameraResources()
    }

    private fun releaseCameraResources() {
        try {
            imageAnalyzer?.clearAnalyzer()
            cameraProvider?.unbindAll()
            cameraExecutor.shutdownNow()
        } catch (e: Exception) {
            Log.e("CameraFragment", "Failed to release camera resources: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseCameraResources()
    }

    override fun onEmptyDetect() {
        fragmentCameraBinding.overlay.invalidate()
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        activity?.runOnUiThread {
            val laneBitmap = generateLaneBitmap()
            _fragmentCameraBinding?.let { fragmentCameraBinding ->
                fragmentCameraBinding.inferenceTime.text = "${inferenceTime}ms"
                fragmentCameraBinding.overlay.apply {
                    setResults(boundingBoxes, laneBitmap)
                    invalidate()
                }

                // 차선 이탈
                // 차량 클래스로 분류된 바운딩 박스를 찾음 (예: "car")
                val vehicleBoundingBox = boundingBoxes.firstOrNull { it.clsName in listOf("일반차량") }

                if (vehicleBoundingBox != null) {
                    // 차량 위치 계산
                    val vehiclePosition = laneDetectionHelper.getCurrentVehiclePosition(vehicleBoundingBox)

                    // 차선 정보 가져오기
                    val edges = laneDetectionHelper.detectEdges(rotatedBitmap)
                    val roi = laneDetectionHelper.regionOfInterest(edges)
                    val lanes = laneDetectionHelper.detectLanes(roi)

                    // 차선 이탈 여부 체크
                    val isLaneDeparture = laneDetectionHelper.checkLaneDeparture(vehiclePosition, lanes)

                    if (isLaneDeparture) {
                        Log.d("LaneDetection", "차선 이탈 감지됨")
                        fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "차선을 이탈했습니다."
                        // tts?.speak("차선을 이탈했습니다.", TextToSpeech.QUEUE_ADD, null, null)
                    }
                } else {
                    Log.d("LaneDetection", "차량이 감지되지 않음")
                }


                if (boundingBoxes.size >= 2) { // 두 개 이상의 차량 감지될 때
                    val box1 = boundingBoxes[0]
                    val box2 = boundingBoxes[1]
                    val imageHeight = fragmentCameraBinding.viewFinder.height.toFloat()

                    // 3. 전방 차량 출발
                    checkFrontVehicleMovement(box1, tts, imageHeight)

                    // 4. 안전 거리
                    checkSafetyDistance(box1, box2, tts, imageHeight)

                    // 거리 로그 출력 (디버깅용)
                    val distance = calculateDistanceBetweenVehicles(box1, box2, imageHeight)
                    Log.d("VehicleDistance", "차량 간 거리: $distance")
                }

                // 5. 신호 감지
                handleTrafficLightDetection(boundingBoxes, tts)

                // laneDetectionHelper.checkLaneEnding(tts) // 차선 변경
                // checkSpeedLimit()          // TODO: 제한 속도 초과

            }
        }
    }

    private fun showEmergencyPopup() {
        val inflater: LayoutInflater = layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_accident, null)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        val dialog = builder.create()

        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)
        val btnNo = dialogView.findViewById<Button>(R.id.btnNo)

        btnYes.setOnClickListener {
            makeEmergencyCall("119")  // 119를 예시로 사용
            dialog.dismiss()
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CALL_PHONE), 1)
        } else {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$phoneNumber")
            startActivity(callIntent)
        }
    }

    // 전방 차량 출발
    private fun checkFrontVehicleMovement(box: BoundingBox, tts: TextToSpeech?, height: Float) {
        val currentY = box.y2 * height  // box의 현재 y 좌표 (화면 기준)
        val movementThreshold = 10.0f

        // 이전 위치가 존재할 때만 출발 감지 수행
        previousY?.let { previousY ->
            // 이전 위치에서 현재 위치로의 변화가 일정 거리 이상이면 차량이 출발한 것으로 간주
            if ((previousY - currentY) > movementThreshold) {
                Log.d("Front", "전방 차량 출발")
                fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "전방 차량이 출발했습니다."
                // tts?.speak("전방 차량이 출발했습니다.", TextToSpeech.QUEUE_ADD, null, null)
            }
        }

        // 현재 위치를 이전 위치로 업데이트
        previousY = currentY
    }

    // 안전 거리
    private fun checkSafetyDistance(box1: BoundingBox, box2: BoundingBox, tts: TextToSpeech?, height: Float) {
        val distance = calculateDistanceBetweenVehicles(box1, box2, height)
        val distanceThreshold = 200.0f

        if (distance < distanceThreshold) {
            if (isDisplayingSafetyMessage == false) {
                numDistance++
            }
            fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "안전 거리를 확보하세요."

            isDisplayingSafetyMessage = true

            // Optionally, reset the flag after a certain delay to allow future messages
            fragmentCameraBinding.bottomSheetLayout.navigationMessage.postDelayed({
                isDisplayingSafetyMessage = false
            }, 10000)
        }
    }

    fun calculateDistanceBetweenVehicles(box1: BoundingBox, box2: BoundingBox, height: Float): Float {
        // 바운딩 박스의 bottom y 좌표를 사용하여 거리 계산
        val y1 = box1.y2 * height  // box1의 하단 y 좌표 (화면 기준)
        val y2 = box2.y2 * height  // box2의 하단 y 좌표 (화면 기준)

        // y 좌표 차이를 계산하여 상대적인 거리 추정 (절대값으로 거리 계산)
        return kotlin.math.abs(y1 - y2)
    }

    // 신호등 감지 및 TTS 안내
    private fun handleTrafficLightDetection(boundingBoxes: List<BoundingBox>, tts: TextToSpeech?) {
        var currentTrafficLightState: String? = null

        for (box in boundingBoxes) {
            when (box.clsName) {
                "초록불" -> {
                    fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "초록불입니다."
                    Log.d("TrafficLightDetection", "초록불 감지됨")
                    currentTrafficLightState = "초록불"
                }
                "빨간불" -> {
                    fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "빨간불입니다."
                    Log.d("TrafficLightDetection", "빨간불 감지됨")
                    currentTrafficLightState = "빨간불"

                    // If red light is detected and the vehicle is moving, show the warning
                    if (isVehicleMoving) {
                        if (isDisplayingSafetyMessage == false) {
                            numSignal++
                        }
                        fragmentCameraBinding.bottomSheetLayout.navigationMessage.text = "빨간불에는 주행하면 안됩니다."

                        isDisplayingSafetyMessage = true

                        // Optionally, reset the flag after a certain delay to allow future messages
                        fragmentCameraBinding.bottomSheetLayout.navigationMessage.postDelayed({
                            isDisplayingSafetyMessage = false
                        }, 10000)

                        Log.d("TrafficLightDetection", "빨간불에 주행 감지됨 - 경고 메시지 출력")
                    }
                }
                else -> {
                    Log.d("TrafficLightDetection", "신호등 상태 감지되지 않음")
                }
            }
        }

        // Store the current traffic light state for further checks
        previousTrafficLightState = currentTrafficLightState
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}