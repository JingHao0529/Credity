package com.example.credity

data class ExpenseItem(
    val id: String = "",
    val name: String = "",
    val amountRM: Double = 0.0,
    val dueDateDays: Int = 0,
    val dueDateText: String = "",
    val isRecurring: Boolean = true,
    val isPaid: Boolean = false
)

data class UserFinancialProfile(
    val userId: String = "user_default",
    val name: String = "Alex Morgan",
    val ageRange: String = "25-34",
    val gender: String = "Male",
    val creditCardInterestRate: Double = 24.0, // 24% APR
    val incomePerMonth: Double = 5000.0, // Base Salary RM 5,000.00
    val totalSpentThisMonthRM: Double = 0.0, // Cumulative Spend
    val riskLevel: String = "Moderate",
    val financialGoals: String = "Avoid Credit Card Debt & Improve Credit Score",
    val learnedInsights: List<String> = listOf(
        "Monthly base salary: RM 5,000.00. Fixed expenses: RM 2,350.00.",
        "Monthly safe buffer: RM 2,650.00.",
        "Goal: Prevent credit card debt and optimize credit score."
    ),
    val quizAnswers: List<String> = emptyList(),
    val expenses: List<ExpenseItem> = listOf(
        ExpenseItem("1", "Rent", 1450.0, 12, "12 days left", isRecurring = true, isPaid = false),
        ExpenseItem("2", "Utilities", 300.0, 5, "5 days left", isRecurring = true, isPaid = false),
        ExpenseItem("3", "Groceries", 600.0, 2, "2 days left", isRecurring = false, isPaid = false)
    )
) {
    val totalExpensesRM: Double
        get() = expenses.sumOf { it.amountRM }

    // Allows negative value if overspent
    val safeToSpendValueRM: Double
        get() = incomePerMonth - totalExpensesRM - totalSpentThisMonthRM

    // Base set daily allowance = (Initial Income - Initial Expenses) / 31
    val baseDailyAllowanceRM: Double
        get() = maxOf(0.0, (incomePerMonth - totalExpensesRM) / 31.0)

    // Daily safe to spend deducts directly based on spendings for the day (minimum floor 0)
    val dailySafeToSpendRM: Double
        get() = maxOf(0.0, baseDailyAllowanceRM - totalSpentThisMonthRM)
}
