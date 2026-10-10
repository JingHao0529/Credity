package com.example.credity

import kotlin.math.roundToInt

data class ScoreResult(
    val score: Int,
    val statusLabel: String, // "Healthy", "Watch it", "Slow down"
    val colorHex: Long,      // 0xFF123C2A, 0xFF4A3B10, 0xFF4D1B22
    val textHex: Long,       // 0xFF4ECA80, 0xFFF3B33E, 0xFFE55353
    val dailyAllowanceRM: Double
)

object DebtScoreCalculator {

    /**
     * Sensitive Debt Score Formula:
     * - 100: No additional expenses yet (full base safe budget available).
     * - 20: 0 monthly safe limit left.
     * - Scaling below 20 down to 0 if negative / overspent.
     *
     * Daily allowance set value = (Income - Fixed Expenses) / 31, deducting spentThisMonthRM.
     */
    fun calculateScore(
        incomeRM: Double = 5000.0,
        fixedExpensesRM: Double = 2350.0,
        spentThisMonthRM: Double = 0.0
    ): ScoreResult {
        val baseSafeBuffer = maxOf(1.0, incomeRM - fixedExpensesRM) // e.g. RM 2,650
        val remainingSafeBuffer = incomeRM - fixedExpensesRM - spentThisMonthRM

        val scoreDouble = when {
            remainingSafeBuffer >= baseSafeBuffer -> 100.0
            remainingSafeBuffer >= 0.0 -> {
                // Scale linearly from 20 (at 0) to 100 (at baseSafeBuffer)
                20.0 + 80.0 * (remainingSafeBuffer / baseSafeBuffer)
            }
            else -> {
                // Scale linearly below 20 down to 0 if negative
                maxOf(0.0, 20.0 + 20.0 * (remainingSafeBuffer / baseSafeBuffer))
            }
        }

        val finalScore = scoreDouble.roundToInt().coerceIn(0, 100)

        // Set daily allowance = (Income - Expenses) / 31, deducting spendings
        val baseDailyAllowance = maxOf(0.0, baseSafeBuffer / 31.0)
        val dailyAllowance = maxOf(0.0, baseDailyAllowance - spentThisMonthRM)

        return when {
            finalScore >= 65 -> ScoreResult(
                score = finalScore,
                statusLabel = "Healthy",
                colorHex = 0xFF123C2A,
                textHex = 0xFF4ECA80,
                dailyAllowanceRM = dailyAllowance
            )
            finalScore >= 40 -> ScoreResult(
                score = finalScore,
                statusLabel = "Watch it",
                colorHex = 0xFF4A3B10,
                textHex = 0xFFF3B33E,
                dailyAllowanceRM = dailyAllowance
            )
            else -> ScoreResult(
                score = finalScore,
                statusLabel = "Slow down",
                colorHex = 0xFF4D1B22,
                textHex = 0xFFE55353,
                dailyAllowanceRM = dailyAllowance
            )
        }
    }
}
