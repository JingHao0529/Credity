package com.example.credity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.credity.ui.theme.CredityTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    SPEND_RESULT,
    SETTINGS,
    AI_CHAT,
    SEE_MORE_DETAILS
}

class MainActivity : ComponentActivity() {

    private var initialScreenOverride = mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Schedule Hourly Google Spreadsheet Sync
        SpreadsheetSyncWorker.scheduleHourlySync(this)

        // Refresh App Widget recalculating Debt Score on launch
        CredityWidgetProvider.updateAllWidgets(this)

        val openScreenExtra = intent?.getStringExtra("extra_open_screen")
        if (openScreenExtra == "AI_CHAT") {
            initialScreenOverride.value = Screen.AI_CHAT
        }

        setContent {
            CredityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CredityApp(
                        initialScreen = initialScreenOverride.value ?: Screen.HOME,
                        onScreenHandled = { initialScreenOverride.value = null }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh App Widget recalculating Debt Score when app is resumed
        CredityWidgetProvider.updateAllWidgets(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val openScreenExtra = intent.getStringExtra("extra_open_screen")
        if (openScreenExtra == "AI_CHAT") {
            initialScreenOverride.value = Screen.AI_CHAT
        }
    }
}

@Composable
fun CredityApp(
    initialScreen: Screen = Screen.HOME,
    onScreenHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseRepo = remember { FirebaseRepository() }

    var currentScreen by remember { mutableStateOf(initialScreen) }
    var spendAmountCents by remember { mutableLongStateOf(0L) }

    // Global Financial State (Formula: Safe to Spend = Income - Fixed Expenses - Total Spent This Month)
    var incomeRM by remember { mutableDoubleStateOf(5000.0) }
    var totalSpentThisMonthRM by remember { mutableDoubleStateOf(0.0) }

    val expensesList = remember {
        mutableStateListOf(
            ExpenseItem("1", "Rent", 1450.0, 12, "12 days left", isRecurring = true, isPaid = false),
            ExpenseItem("2", "Utilities", 300.0, 5, "5 days left", isRecurring = true, isPaid = false),
            ExpenseItem("3", "Groceries", 600.0, 2, "2 days left", isRecurring = false, isPaid = false)
        )
    }

    var userProfile by remember {
        mutableStateOf(UserFinancialProfile(incomePerMonth = incomeRM, expenses = expensesList))
    }

    val totalExpensesRM = expensesList.sumOf { it.amountRM }
    // Allows negative value if overspent
    val safeToSpendRM = incomeRM - totalExpensesRM - totalSpentThisMonthRM

    // Helper to persist financial state to SharedPreferences and update App Widget
    fun syncStateToWidget() {
        val prefs = context.getSharedPreferences("credity_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("incomeRM", incomeRM.toFloat())
            .putFloat("totalExpensesRM", totalExpensesRM.toFloat())
            .putFloat("totalSpentThisMonthRM", totalSpentThisMonthRM.toFloat())
            .apply()
        CredityWidgetProvider.updateAllWidgets(context)
    }

    LaunchedEffect(initialScreen) {
        if (initialScreen != Screen.HOME) {
            currentScreen = initialScreen
            onScreenHandled()
        }
    }

    // Refresh widget whenever financial state values change
    LaunchedEffect(incomeRM, totalExpensesRM, totalSpentThisMonthRM) {
        syncStateToWidget()
    }

    // Smooth Animated Navigation Transitions between screens
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
        },
        label = "ScreenNavigationTransition"
    ) { targetNavScreen ->
        when (targetNavScreen) {
            Screen.HOME -> {
                HomeScreen(
                    safeToSpendRM = safeToSpendRM,
                    totalSpentThisMonthRM = totalSpentThisMonthRM,
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                    onNavigateToSpendResult = { amountCents ->
                        spendAmountCents = amountCents
                        currentScreen = Screen.SPEND_RESULT
                    },
                    onNavigateToAiChat = { currentScreen = Screen.AI_CHAT },
                    onNavigateToSeeMore = { currentScreen = Screen.SEE_MORE_DETAILS }
                )
            }
            Screen.SPEND_RESULT -> {
                val closestUpcomingExpense = expensesList
                    .filter { !it.isPaid }
                    .minByOrNull { it.dueDateDays }

                SpendResultScreen(
                    spendAmountCents = spendAmountCents,
                    monthlySafeToSpendRM = safeToSpendRM,
                    upcomingExpense = closestUpcomingExpense,
                    onConfirmSpendAnyway = { spendAmountDollars ->
                        totalSpentThisMonthRM += spendAmountDollars
                        val updatedProfile = userProfile.copy(
                            incomePerMonth = incomeRM,
                            totalSpentThisMonthRM = totalSpentThisMonthRM,
                            expenses = expensesList
                        )
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch {
                            firebaseRepo.saveUserProfile(updatedProfile)
                        }
                        currentScreen = Screen.HOME
                    },
                    onBackClick = { currentScreen = Screen.HOME }
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    incomeRM = incomeRM,
                    totalExpensesRM = totalExpensesRM,
                    safeToSpendRM = safeToSpendRM,
                    onApplyExtractedStatement = { extractedIncome, extractedExpenses ->
                        incomeRM = extractedIncome
                        expensesList.clear()
                        expensesList.addAll(extractedExpenses)
                        val updatedProfile = userProfile.copy(
                            incomePerMonth = extractedIncome,
                            expenses = expensesList
                        )
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch { firebaseRepo.saveUserProfile(updatedProfile) }
                    },
                    onBackClick = { currentScreen = Screen.HOME }
                )
            }
            Screen.AI_CHAT -> {
                AiChatScreen(
                    profile = userProfile.copy(
                        incomePerMonth = incomeRM,
                        totalSpentThisMonthRM = totalSpentThisMonthRM,
                        expenses = expensesList
                    ),
                    onUpdateProfile = { updatedProfile ->
                        userProfile = updatedProfile
                    },
                    onBackClick = { currentScreen = Screen.HOME }
                )
            }
            Screen.SEE_MORE_DETAILS -> {
                SeeMoreDetailsScreen(
                    incomeRM = incomeRM,
                    expensesList = expensesList,
                    totalSpentThisMonthRM = totalSpentThisMonthRM,
                    onUpdateIncome = { newIncome ->
                        incomeRM = newIncome
                        val updatedProfile = userProfile.copy(incomePerMonth = newIncome)
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch { firebaseRepo.saveUserProfile(updatedProfile) }
                    },
                    onUpdateExpenseAmount = { expenseId, newAmount ->
                        val index = expensesList.indexOfFirst { it.id == expenseId }
                        if (index != -1) {
                            expensesList[index] = expensesList[index].copy(amountRM = newAmount)
                            val updatedProfile = userProfile.copy(expenses = expensesList)
                            userProfile = updatedProfile
                            syncStateToWidget()
                            coroutineScope.launch { firebaseRepo.saveUserProfile(updatedProfile) }
                        }
                    },
                    onAddExpense = { name, amount, daysLeft ->
                        val newId = (expensesList.size + 1).toString()
                        expensesList.add(ExpenseItem(newId, name, amount, daysLeft, "$daysLeft days left", true, false))
                        val updatedProfile = userProfile.copy(expenses = expensesList)
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch { firebaseRepo.saveUserProfile(updatedProfile) }
                    },
                    onDeleteExpense = { expenseId ->
                        expensesList.removeAll { it.id == expenseId }
                        val updatedProfile = userProfile.copy(expenses = expensesList)
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch { firebaseRepo.saveUserProfile(updatedProfile) }
                    },
                    onTogglePaid = { expenseId, isPaid ->
                        val index = expensesList.indexOfFirst { it.id == expenseId }
                        if (index != -1) {
                            expensesList[index] = expensesList[index].copy(isPaid = isPaid)
                            syncStateToWidget()
                        }
                    },
                    onClearTotalSpentThisMonth = {
                        totalSpentThisMonthRM = 0.0
                        val updatedProfile = userProfile.copy(totalSpentThisMonthRM = 0.0)
                        userProfile = updatedProfile
                        syncStateToWidget()
                        coroutineScope.launch {
                            firebaseRepo.clearTotalSpentThisMonth()
                        }
                    },
                    onBackClick = { currentScreen = Screen.HOME }
                )
            }
        }
    }
}
