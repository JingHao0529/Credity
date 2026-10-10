package com.example.credity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credity.ui.theme.CredityTheme
import kotlinx.coroutines.launch
import java.util.Locale

private val DarkBackground = Color(0xFF121418)
private val CardSurfaceDark = Color(0xFF1B1D22)
private val CardBorderRed = Color(0xFFE53935)
private val WarningRedBadge = Color(0xFFE53935)
private val WarningRedText = Color(0xFFE55353)

private val SafeGreenBadge = Color(0xFF1B3D2B)
private val SafeGreenText = Color(0xFF4ECA80)
private val TextSecondary = Color(0xFF9E9EA5)
private val AccentTeal = Color(0xFF6BC8A6)
private val UserBubbleBg = Color(0xFF2C4C38)

@Composable
fun SpendResultScreen(
    spendAmountCents: Long,
    monthlySafeToSpendRM: Double = 2650.0,
    upcomingExpense: ExpenseItem? = ExpenseItem("1", "Rent", 1450.0, 12, "12 days left", true, false),
    onConfirmSpendAnyway: (Double) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GrafilabAiService() }

    val spendAmountDollars = if (spendAmountCents > 0) spendAmountCents / 100.0 else 84.50

    val displayAmount = remember(spendAmountCents) {
        String.format(Locale.US, "RM %,.2f", spendAmountDollars)
    }

    // Daily safe allowance = Monthly safe / 31 (Total safe to spend divided by 31)
    val dailySafeToSpendRM = remember(monthlySafeToSpendRM) {
        maxOf(0.0, monthlySafeToSpendRM / 31.0)
    }

    val remainingDailySafe = remember(dailySafeToSpendRM, spendAmountDollars) {
        maxOf(0.0, dailySafeToSpendRM - spendAmountDollars)
    }

    val isExceeding = spendAmountDollars > dailySafeToSpendRM

    // Overbudget calculation for 24% interest risk
    val overspentRM = spendAmountDollars - monthlySafeToSpendRM
    val isOverbudgetNegative = overspentRM > 0.0
    val extraInterestCostRM = if (isOverbudgetNegative) overspentRM * 0.24 else 0.0

    // Interactive AI Chatbot Messages & Input State
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var userQuestionText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    // Fast AI Evaluation generated on the spot on load
    LaunchedEffect(spendAmountDollars) {
        if (chatMessages.isEmpty()) {
            isAiLoading = true
            val initialEvaluation = if (isOverbudgetNegative) {
                String.format(
                    Locale.US,
                    "🐧 **Penny's Critical Overbudget Warning:**\n\n" +
                            "🚨 **Negative Safe Buffer Triggered**\n" +
                            "• **Over Budget By:** **RM %,.2f**\n" +
                            "• **24%% Interest Rate Extra Cost (Month 1):** **RM %,.2f**\n\n" +
                            "⚠️ **Debt Penalty Risk:**\n" +
                            "• Spending **RM %,.2f** depletes your monthly buffer into negative territory.\n" +
                            "• The overspent **RM %,.2f** carries a 24%% interest penalty, adding **RM %,.2f** in extra charges in month 1.\n\n" +
                            "💡 *Ask me any questions below if you need advice!*",
                    overspentRM,
                    extraInterestCostRM,
                    spendAmountDollars,
                    overspentRM,
                    extraInterestCostRM
                )
            } else if (isExceeding) {
                String.format(
                    Locale.US,
                    "🐧 **Penny's Warning & Forecast:**\n\n" +
                            "🚨 **Caution: Daily Limit Exceeded**\n" +
                            "• **Requested Amount:** RM %,.2f\n" +
                            "• **Daily Allowance:** RM %,.2f\n" +
                            "• **Over Daily Limit By:** **RM %,.2f**\n\n" +
                            "📊 **Impact on Monthly Budget:**\n" +
                            "• Your daily allowance for the remaining days drops to **RM %,.2f/day**.\n\n" +
                            "💡 *Ask me any questions below if you need advice!*",
                    spendAmountDollars,
                    dailySafeToSpendRM,
                    spendAmountDollars - dailySafeToSpendRM,
                    maxOf(0.0, (monthlySafeToSpendRM - spendAmountDollars) / 31.0)
                )
            } else {
                String.format(
                    Locale.US,
                    "🐧 **Penny's Approval Forecast:**\n\n" +
                            "✅ **Safe Purchase Approved!**\n" +
                            "• **Requested Amount:** RM %,.2f\n" +
                            "• **Daily Allowance:** RM %,.2f\n" +
                            "• **Remaining Today:** **RM %,.2f**\n\n" +
                            "📊 **Financial Impact:**\n" +
                            "• This purchase fits comfortably within your daily budget.\n" +
                            "• **Zero Risk** of incurring credit card interest charges.\n\n" +
                            "💡 *Ask me any questions below if you need advice!*",
                    spendAmountDollars,
                    dailySafeToSpendRM,
                    remainingDailySafe
                )
            }
            chatMessages.add(ChatMessage("AI", initialEvaluation))
            isAiLoading = false
        }
    }

    fun sendUserQuestion(prompt: String) {
        if (prompt.isBlank() || isAiLoading) return

        chatMessages.add(ChatMessage("User", prompt))
        userQuestionText = ""
        isAiLoading = true

        coroutineScope.launch {
            val historyPairs = chatMessages.map { Pair(it.sender, it.text) }
            val customPrompt = "Regarding my requested purchase of RM $spendAmountDollars (monthly safe buffer RM $monthlySafeToSpendRM): $prompt"
            val dummyProfile = UserFinancialProfile(
                incomePerMonth = 5000.0,
                totalSpentThisMonthRM = 0.0
            )

            val result = aiService.getPersonalizedAiAdvice(
                userMessage = customPrompt,
                profile = dummyProfile,
                chatHistory = historyPairs
            )
            isAiLoading = false

            result.onSuccess { response ->
                chatMessages.add(ChatMessage("AI", response.adviceText))
            }.onFailure {
                chatMessages.add(ChatMessage("AI", "🐧 **Penny's Advice:**\n• Spending RM $spendAmountDollars will reduce your monthly buffer to RM ${String.format(Locale.US, "%.2f", maxOf(0.0, monthlySafeToSpendRM - spendAmountDollars))}.\n• I recommend keeping a positive safe buffer for upcoming bills!"))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Header: "You're about to spend"
        Text(
            text = "You're about to spend",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Large Amount
        Text(
            text = displayAmount,
            style = TextStyle(
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Dynamic Warning Card ("Are you sure about this") - ONLY SHOWN IF EXCEEDING DAILY ALLOWANCE
        if (isExceeding) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.5.dp, CardBorderRed),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = CardSurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Warning Section Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(WarningRedBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Are you sure about this",
                                style = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "You'll exceed your daily allowance of RM %,.2f.",
                                    dailySafeToSpendRM
                                ),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningRedText
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    // Daily Allowance Remaining Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SafeGreenBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Safe Shield",
                                tint = SafeGreenText,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "Daily Allowance Remaining: RM %,.2f", remainingDailySafe),
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreenText
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // CONDITIONAL DISPLAY: 24% Interest Risk Card if Overspent Negative OR Upcoming Expense Card
        if (isOverbudgetNegative) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.5.dp, CardBorderRed), shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardSurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WarningRedBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Percent,
                                contentDescription = "24% Interest Penalty",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "24% Interest Overbudget Cost",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningRedText
                                )
                            )
                            Text(
                                text = "Negative Safe Buffer Penalty",
                                style = TextStyle(fontSize = 11.sp, color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount Over Budget:",
                            style = TextStyle(fontSize = 13.sp, color = TextSecondary)
                        )
                        Text(
                            text = String.format(Locale.US, "RM %,.2f", overspentRM),
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Est. 1st Month 24% Interest Extra Cost:",
                            style = TextStyle(fontSize = 13.sp, color = TextSecondary)
                        )
                        Text(
                            text = String.format(Locale.US, "+RM %,.2f", extraInterestCostRM),
                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarningRedText)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Upcoming Pending Payment Card
            upcomingExpense?.let { expense ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardSurfaceDark
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Upcoming Expense",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = expense.name,
                                            style = TextStyle(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = " in ${expense.dueDateDays} days",
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                    Text(
                                        text = "Next Pending Payment",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    )
                                }
                            }

                            Text(
                                text = String.format(Locale.US, "RM %,.2f", expense.amountRM),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Bar
                        val progressFraction = (expense.dueDateDays.toFloat() / 30f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(SafeGreenText)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // INTERACTIVE AI CHATBOT CARD AT BOTTOM
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐧", fontSize = 16.sp)
                    }

                    Text(
                        text = "PENNY THE PENGUIN'S AI SPENDING CHATBOT",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentTeal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chat Messages Feed with Typewriter Animation
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chatMessages.forEach { msg ->
                        if (msg.sender == "User") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = UserBubbleBg
                                ) {
                                    Text(
                                        text = parseMarkdownToAnnotatedString(msg.text),
                                        style = TextStyle(fontSize = 14.sp, color = Color.White),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.05f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        TypewriterText(
                                            text = msg.text,
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                lineHeight = 21.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isAiLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AccentTeal,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Penny is calculating advice on the spot...",
                                style = TextStyle(fontSize = 12.sp, color = TextSecondary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive AI Prompt Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userQuestionText,
                        onValueChange = { userQuestionText = it },
                        placeholder = { Text("Ask Penny: What if I pay half now?", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedPlaceholderColor = TextSecondary,
                            unfocusedPlaceholderColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { sendUserQuestion(userQuestionText) },
                        enabled = userQuestionText.isNotBlank() && !isAiLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (userQuestionText.isNotBlank()) AccentTeal else Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Question",
                            tint = if (userQuestionText.isNotBlank()) Color(0xFF101E17) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Go Back",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    onConfirmSpendAnyway(spendAmountDollars)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isExceeding) CardBorderRed else AccentTeal)
            ) {
                Text(
                    text = if (isExceeding) "Spend Anyway" else "Confirm Spend",
                    color = if (isExceeding) Color.White else Color(0xFF101E17),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SpendResultScreenPreview() {
    CredityTheme {
        SpendResultScreen(
            spendAmountCents = 8450L,
            monthlySafeToSpendRM = 250.0,
            onBackClick = {}
        )
    }
}
