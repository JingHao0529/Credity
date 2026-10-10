package com.example.credity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Stores the complete user financial profile & learned AI insights in the Firebase Firestore database.
     */
    suspend fun saveUserProfile(profile: UserFinancialProfile = UserFinancialProfile()): Result<String> {
        return try {
            val userMap = mapOf(
                "name" to profile.name,
                "ageRange" to profile.ageRange,
                "gender" to profile.gender,
                "creditCardInterestRate" to profile.creditCardInterestRate,
                "incomePerMonth" to profile.incomePerMonth,
                "totalSpentThisMonthRM" to profile.totalSpentThisMonthRM,
                "predictedExpensesPerMonth" to profile.totalExpensesRM,
                "safeToSpendValue" to profile.safeToSpendValueRM,
                "riskLevel" to profile.riskLevel,
                "financialGoals" to profile.financialGoals,
                "learnedInsights" to profile.learnedInsights,
                "quizAnswers" to profile.quizAnswers,
                "expenses" to profile.expenses.map { expense ->
                    mapOf(
                        "id" to expense.id,
                        "name" to expense.name,
                        "amountRM" to expense.amountRM,
                        "dueDateDays" to expense.dueDateDays,
                        "dueDateText" to expense.dueDateText,
                        "isRecurring" to expense.isRecurring,
                        "isPaid" to expense.isPaid
                    )
                },
                "updatedAt" to Timestamp.now()
            )

            db.collection("user_profiles")
                .document(profile.userId)
                .set(userMap, SetOptions.merge())
                .await()

            Result.success("Saved to Firebase Firestore ('user_profiles', docId: '${profile.userId}')")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears cumulative total spent this month from Firebase Firestore forever.
     */
    suspend fun clearTotalSpentThisMonth(userId: String = "user_default"): Result<Unit> {
        return try {
            db.collection("user_profiles")
                .document(userId)
                .update("totalSpentThisMonthRM", 0.0)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the user profile from Firebase Firestore database.
     */
    suspend fun getUserProfile(userId: String = "user_default"): Result<UserFinancialProfile?> {
        return try {
            val document = db.collection("user_profiles")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Result.success(null)
                @Suppress("UNCHECKED_CAST")
                val rawExpenses = data["expenses"] as? List<Map<String, Any>> ?: emptyList()
                val expensesList = rawExpenses.map { exp ->
                    ExpenseItem(
                        id = exp["id"] as? String ?: "",
                        name = exp["name"] as? String ?: "",
                        amountRM = (exp["amountRM"] as? Number)?.toDouble() ?: 0.0,
                        dueDateDays = (exp["dueDateDays"] as? Number)?.toInt() ?: 0,
                        dueDateText = exp["dueDateText"] as? String ?: "",
                        isRecurring = exp["isRecurring"] as? Boolean ?: true,
                        isPaid = exp["isPaid"] as? Boolean ?: false
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val insights = data["learnedInsights"] as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val answers = data["quizAnswers"] as? List<String> ?: emptyList()

                val profile = UserFinancialProfile(
                    userId = userId,
                    name = data["name"] as? String ?: "Alex Morgan",
                    ageRange = data["ageRange"] as? String ?: "25-34",
                    gender = data["gender"] as? String ?: "Male",
                    creditCardInterestRate = (data["creditCardInterestRate"] as? Number)?.toDouble() ?: 15.0,
                    incomePerMonth = (data["incomePerMonth"] as? Number)?.toDouble() ?: 5000.0,
                    totalSpentThisMonthRM = (data["totalSpentThisMonthRM"] as? Number)?.toDouble() ?: 0.0,
                    riskLevel = data["riskLevel"] as? String ?: "Moderate",
                    financialGoals = data["financialGoals"] as? String ?: "Avoid Credit Card Debt & Improve Credit Score",
                    learnedInsights = if (insights.isNotEmpty()) insights else UserFinancialProfile().learnedInsights,
                    quizAnswers = answers,
                    expenses = expensesList
                )
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
