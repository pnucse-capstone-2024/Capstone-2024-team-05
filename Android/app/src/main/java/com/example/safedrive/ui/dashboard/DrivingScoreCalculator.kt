package com.example.safedrive.ui.dashboard

import com.example.safedrive.R

class DrivingScoreCalculator {

    fun calculateTotalScore(numSudden: Int, numDistance: Int, numSignal: Int): Int {

        val scores = calculateDrivingScores(numSudden, numDistance, numSignal)

        val totalScore = (scores.sum() / (9f * scores.size)) * 100
        return totalScore.toInt().coerceIn(0, 100) // 0 ~ 100 사이로 제한
    }

    fun calculateDrivingScores(numSudden: Int, numDistance: Int, numSignal: Int): List<Float> {
        // 운전 안전성 점수 (급정거, 안전거리 유지)
        val safetyScore = calculateSafetyScore(numSudden, numDistance)

        // 교통 법규 준수 점수 (신호 위반, 속도 위반)
        val lawAbidingScore = calculateLawAbidingScore(numSignal)

        // 운전 습관 점수 (급가속, 차선 변경)
        val habitScore = calculateHabitScore(numSudden)

        // 운전 환경 대응 점수 (도로 상황에 맞춘 주행, 장애물 감지 및 회피 반응)
        val environmentResponseScore = calculateEnvironmentResponseScore(numDistance, numSignal)

        return listOf(
            safetyScore,
            lawAbidingScore,
            habitScore,
            environmentResponseScore
        )
    }

    // 운전 안전성 점수 계산
    private fun calculateSafetyScore(numSudden: Int, numDistance: Int): Float {
        var score = 9f
        score -= numSudden * 1.5f
        score -= numDistance * 1.0f
        return score.coerceIn(1f, 9f)
    }

    // 교통 법규 준수 점수 계산
    private fun calculateLawAbidingScore(numSignal: Int): Float {
        var score = 9f
        score -= numSignal * 2.0f
        return score.coerceIn(1f, 9f)
    }

    // 운전 습관 점수 계산 (급가속 및 차선 변경 고려)
    private fun calculateHabitScore(numSudden: Int): Float {
        var score = 9f
        score -= numSudden * 1.2f
        return score.coerceIn(1f, 9f)
    }

    // 운전 환경 대응 점수 계산
    private fun calculateEnvironmentResponseScore(numDistance: Int, numSignal: Int): Float {
        var score = 9f
        score -= (numDistance + numSignal) * 0.8f
        return score.coerceIn(1f, 9f)
    }

    fun provideDrivingEmoji(numSudden: Int, numDistance: Int, numSignal: Int): Int {
        var emojiSrc = 0
        val total = numSudden + numDistance + numSignal
        when {
            total < 1 -> {
                emojiSrc = R.drawable.ic_emoji_1
            }
            total in 1..2 -> {
                emojiSrc = R.drawable.ic_emoji_2
            }
            total in 2..3 -> {
                emojiSrc = R.drawable.ic_emoji_3
            }
            total in 3..4 -> {
                emojiSrc= R.drawable.ic_emoji_4
            }
            total > 4 -> {
                emojiSrc = R.drawable.ic_emoji_5
            }
        }
        return emojiSrc
    }
    fun provideDrivingFeedback(numSudden: Int, numDistance: Int, numSignal: Int): String {
        val totalScore = numSudden + numDistance + numSignal

        val feedback = StringBuilder()

        // 기본 피드백
        when {
            totalScore == 0 -> {
                feedback.append("완벽한 운전 습관이야! 계속 이렇게만 하면 돼.너무 잘하고 있어! 😊 ")
            }
            totalScore in 1..3 -> {
                feedback.append("운전 진짜 잘하고 있는데, 조금만 더 신경 쓰면 금방 완벽해질 것 같아! 👍 ")
            }
            totalScore in 4..6 -> {
                feedback.append("조금만 조심하면 더 안전할 것 같아. 항상 안전이 제일이니까! 🚗💨 ")
            }
            totalScore > 6 -> {
                feedback.append("운전 습관을 조금 바꾸면 더 안전할 수 있을 것 같아. 안전이 제일 중요하잖아! 😇 ")
            }
        }

        // 각 항목 중 가장 안 좋은 항목에 대한 피드백 (총점 2 이상일 때)
        if (totalScore > 2) {
            val details = mutableListOf<String>()

            // 가장 안 좋은 항목들 추가
            val maxScore = maxOf(numSudden, numDistance, numSignal)

            if (numSudden == maxScore && numSudden > 0) {
                details.add("급정거는 좀 위험할 수 있어. 천천히 멈추는 게 더 좋아!")
            }

            if (numDistance == maxScore && numDistance > 0) {
                details.add("차간 거리를 조금만 더 유지해 주면 더 안전할 거야.")
            }

            if (numSignal == maxScore && numSignal > 0) {
                details.add("신호는 꼭 지켜야 해! 안전이 제일 중요하니까.")
            }

            // 가장 안 좋은 항목이 있으면 추가
            if (details.isNotEmpty()) {
                feedback.append(" ").append(details.joinToString(" "))
            }
        }

        return feedback.toString()
    }
}