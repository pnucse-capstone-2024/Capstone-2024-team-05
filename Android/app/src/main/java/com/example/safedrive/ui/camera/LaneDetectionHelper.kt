package com.example.safedrive.ui.camera

import android.graphics.Bitmap
import org.opencv.android.Utils
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.atan

class LaneDetectionHelper {

    // Canny edge detection
    fun detectEdges(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)  // 비트맵을 OpenCV Mat 형식으로 변환
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)  // Grayscale 변환
        Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0) // 가우시안 블러로 노이즈 제거
        Imgproc.Canny(mat, mat, 100.0, 200.0)  // Canny 엣지 감지
        return mat
    }

    // 관심 영역 설정 (ROI)
    fun regionOfInterest(mat: Mat): Mat {
        val mask = Mat.zeros(mat.size(), CvType.CV_8UC1)

        val vertices = MatOfPoint(
            Point(0.0, mat.rows().toDouble()),
            Point((mat.cols() * 0.5).toDouble(), (mat.rows() * 0.5).toDouble()),
            // Point((mat.cols() * 0.4).toDouble(), (mat.rows() * 0.6).toDouble()),
            // Point((mat.cols() * 0.6).toDouble(), (mat.rows() * 0.6).toDouble()),
            Point(mat.cols().toDouble(), mat.rows().toDouble())
        )

        Imgproc.fillPoly(mask, listOf(vertices), Scalar(255.0))  // ROI 영역을 설정
        val masked = Mat()
        Core.bitwise_and(mat, mask, masked)
        return masked
    }

//    // Hough Line Transform으로 차선 인식
//    fun detectLanes(mat: Mat): List<Pair<Point, Point>> {
//        val lines = Mat()
//        Imgproc.HoughLinesP(mat, lines, 1.0, Math.PI / 180.0, 100, 50.0, 200.0)
//        val result = mutableListOf<Pair<Point, Point>>()
//        for (i in 0 until lines.rows()) {
//            val line = lines[i, 0]
//            val pt1 = Point(line[0], line[1])
//            val pt2 = Point(line[2], line[3])
//            result.add(Pair(pt1, pt2))
//        }
//        return result
//    }


    fun detectLanes(mat: Mat): List<Pair<Point, Point>> {
        val lines = Mat()
        Imgproc.HoughLinesP(mat, lines, 6.0, Math.PI / 60.0, 160, 40.0, 25.0)
        val result = mutableListOf<Pair<Point, Point>>()

        for (i in 0 until lines.rows()) {
            val line = lines[i, 0]
            val pt1 = Point(line[0], line[1])  // 직선의 시작점
            val pt2 = Point(line[2], line[3])  // 직선의 끝점

            // 기울기 계산: 기울기가 매우 크면 수직에 가깝다고 판단
            val deltaX = pt2.x - pt1.x
            val deltaY = pt2.y - pt1.y

            // deltaX가 0에 가까우면 기울기가 매우 커서 수직에 가까움
            if (deltaX != 0.0) {
                val slope = deltaY / deltaX
                val angle = Math.toDegrees(atan(slope))

                // 수직인 선: 기울기 각도가 75도 이상 (세로에 가까움)
                if (angle in -90.0..-40.0 || angle in 40.0..90.0) {
                    result.add(Pair(pt1, pt2))  // 수직에 가까운 선만 추가
                }
            } else {
                // deltaX == 0인 경우 완전히 수직인 선
                result.add(Pair(pt1, pt2))
            }
        }

        return result
    }

    // 인식한 차선을 비트맵에 그리기 (차선 선만 그리기)
    fun drawLaneLines(bitmap: Bitmap, lanes: List<Pair<Point, Point>>): Bitmap {
        val resultBitmap = bitmap.copy(bitmap.config, true)
        val canvas = Canvas(resultBitmap)

        // 각 차선을 그리는 부분
        val linePaint = Paint().apply {
            color = Color.GREEN  // 선의 색상 설정
            style = Paint.Style.STROKE  // 선 그리기
            strokeWidth = 10f  // 선의 두께 설정
        }

        for (lane in lanes) {
            val startX = lane.first.x
            val startY = lane.first.y
            val endX = lane.second.x
            val endY = lane.second.y

            // 기울기 계산 (x 변화량이 0에 가까운 선은 수직에 가까움)
            val slope = (endY - startY) / (endX - startX)

            // 수직에 가까운 선만 그리기 (기울기 절댓값이 0.6과 0.9 사이인 경우만 그리기)
            if ((Math.abs(slope) > 0.6) && (Math.abs(slope) < 0.9)) {
                canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), linePaint)

            }
        }
        return resultBitmap
    }

//    // 인식한 차선을 비트맵에 그리기 (차선 사이 영역만 색칠)
//    fun drawLaneArea(bitmap: Bitmap, lanes: List<Pair<Point, Point>>): Bitmap {
//        val resultBitmap = bitmap.copy(bitmap.config, true)
//        val canvas = Canvas(resultBitmap)
//
//        // 차선 사이의 영역을 색칠하는 부분
//        // if (lanes.size >= 2) {
//            val leftLane = lanes.minByOrNull { it.first.x }!!
//            val rightLane = lanes.maxByOrNull { it.second.x }!!
//
//            val path = Path()
//            path.moveTo(leftLane.first.x.toFloat(), leftLane.first.y.toFloat())
//            path.lineTo(leftLane.second.x.toFloat(), leftLane.second.y.toFloat())
//            path.lineTo(rightLane.second.x.toFloat(), rightLane.second.y.toFloat())
//            path.lineTo(rightLane.first.x.toFloat(), rightLane.first.y.toFloat())
//            path.close()
//
//            val areaPaint = Paint().apply {
//                color = Color.GREEN
//                style = Paint.Style.FILL
//                alpha = 100 // 투명도 설정
//            }
//
//            canvas.drawPath(path, areaPaint)
//        //}
//
//        return resultBitmap
//    }

    // 차선 이탈 여부 체크
    fun checkLaneDeparture(vehiclePosition: Point, lanes: List<Pair<Point, Point>>): Boolean {
        // 차선의 왼쪽 경계와 오른쪽 경계를 계산
        for (lane in lanes) {
            val laneStart = lane.first
            val laneEnd = lane.second

            // 차선의 기울기와 차량의 위치 관계를 계산
            val slope = (laneEnd.y - laneStart.y) / (laneEnd.x - laneStart.x)
            val intercept = laneStart.y - slope * laneStart.x

            // 차량의 y 좌표에서 경계값 계산
            val expectedYAtVehicleX = slope * vehiclePosition.x + intercept

            // 차량이 차선 경계 내에 있는지 확인
            if (vehiclePosition.y < expectedYAtVehicleX - 10 || vehiclePosition.y > expectedYAtVehicleX + 10) {
                // 차선을 벗어난 경우
                return true
            }
        }

        // 차선 이탈이 없으면 false 반환
        return false
    }

    // 차량의 현재 위치를 Point로 가져오는 함수
    fun getCurrentVehiclePosition(boundingBox: BoundingBox): Point {
        // 바운딩 박스의 중심점을 계산
        val centerX = (boundingBox.x1 + boundingBox.x2) / 2
        val centerY = (boundingBox.y1 + boundingBox.y2) / 2

        // 차량 위치를 중심점으로 반환
        return Point(centerX.toDouble(), centerY.toDouble())
    }
}