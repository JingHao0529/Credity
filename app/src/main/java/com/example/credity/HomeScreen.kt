package com.example.credity

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credity.ui.theme.CredityTheme
import kotlinx.coroutines.delay
import java.util.Locale

// Custom Color Palette matching the target UI design
private val DarkGreenGradientTop = Color(0xFF2C4C38)
private val DarkGreenGradientBottom = Color(0xFF18281E)

private val DarkRedGradientTop = Color(0xFF6B1D24)
private val DarkRedGradientBottom = Color(0xFF2E0F13)

// Matching Dynamic Container Colors for Green vs Red budget states
private val DarkGreenContainer = Color(0xFF132219)
private val DarkRedContainer = Color(0xFF2B1014)

private val InputFieldDark = Color(0xFF111F17)

private val AccentTeal = Color(0xFF6BC8A6)
private val AccentPink = Color(0xFFE87A7A)
private val RecurringGold = Color(0xFFDDAE3B)

private val TextSecondary = Color(0xFF9E9EA5)

@Composable
fun HomeScreen(
    incomeRM: Double = 5000.0,
    totalExpensesRM: Double = 2350.0,
    safeToSpendRM: Double = 2650.0,
    totalSpentThisMonthRM: Double = 0.0,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSpendResult: (Long) -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToSeeMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Store amount as total cents for ATM-style currency input
    var cents by remember { mutableLongStateOf(0L) }

    // Reset input keypad whenever the homepage is opened
    LaunchedEffect(Unit) {
        cents = 0L
    }

    // Base Set Daily Allowance = (Monthly Income - Monthly Expenses) / 31 (Recalculates when income/expenses change)
    val baseDailyAllowanceRM = remember(incomeRM, totalExpensesRM) {
        maxOf(0.0, (incomeRM - totalExpensesRM) / 31.0)
    }

    // Daily Left value deducts directly based on spendings for the day (minimum floor 0)
    val dailyLeftRM = remember(baseDailyAllowanceRM, totalSpentThisMonthRM) {
        maxOf(0.0, baseDailyAllowanceRM - totalSpentThisMonthRM)
    }

    // Dynamic Safe to Spend RM value (allows negative value if overspent)
    val dynamicSafeToSpendRM = remember(safeToSpendRM, cents) {
        safeToSpendRM - (cents / 100.0)
    }

    // Current typed purchase amount in RM
    val currentInputRM = cents / 100.0

    // Effective total spent including current input on keypad
    val effectiveSpentRM = totalSpentThisMonthRM + currentInputRM

    // Recalculate score and status dynamically in real-time as keypad digits change!
    val scoreResult = remember(cents, totalSpentThisMonthRM, incomeRM, totalExpensesRM) {
        DebtScoreCalculator.calculateScore(
            incomeRM = incomeRM,
            fixedExpensesRM = totalExpensesRM,
            spentThisMonthRM = effectiveSpentRM
        )
    }

    // Color Meter Relative to Base Safe Buffer (Income - Fixed Expenses)
    val baseSafeBufferRM = maxOf(1.0, incomeRM - totalExpensesRM)

    val safeBufferRatio = remember(dynamicSafeToSpendRM, baseSafeBufferRM) {
        (dynamicSafeToSpendRM / baseSafeBufferRM).toFloat().coerceIn(0f, 1f)
    }

    // Spend fraction (0.0 = Full safe buffer relative to Income - Expenses / Green, 1.0 = Depleted buffer / Red)
    val spendFraction = remember(safeBufferRatio) {
        (1f - safeBufferRatio).coerceIn(0f, 1f)
    }

    // Dynamic gradient colors interpolated between Green and Red
    val targetGradientTop = remember(spendFraction) {
        lerp(DarkGreenGradientTop, DarkRedGradientTop, spendFraction)
    }

    val targetGradientBottom = remember(spendFraction) {
        lerp(DarkGreenGradientBottom, DarkRedGradientBottom, spendFraction)
    }

    val animatedGradientTop by animateColorAsState(
        targetValue = targetGradientTop,
        animationSpec = tween(durationMillis = 300),
        label = "GradientTopAnimation"
    )

    val animatedGradientBottom by animateColorAsState(
        targetValue = targetGradientBottom,
        animationSpec = tween(durationMillis = 300),
        label = "GradientBottomAnimation"
    )

    // Dynamic Bottom Half Container Color interpolating between Green Container & Red Container
    val targetContainerColor = remember(spendFraction) {
        lerp(DarkGreenContainer, DarkRedContainer, spendFraction)
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300),
        label = "ContainerColorAnimation"
    )

    // Dynamic "CAN I AFFORD IT?" Card Color matching the rest of the screen's dynamic color meter
    val targetCardColor = remember(spendFraction) {
        lerp(Color(0xFF1B3325), Color(0xFF38151A), spendFraction)
    }

    val animatedCardColor by animateColorAsState(
        targetValue = targetCardColor,
        animationSpec = tween(durationMillis = 300),
        label = "CardColorAnimation"
    )

    val formattedAmount = remember(cents) {
        val dollars = cents / 100
        val remainderCents = cents % 100
        String.format(Locale.US, "%,d.%02d", dollars, remainderCents)
    }

    // Main header safe to spend amount remains fixed at saved safeToSpendRM until Confirm Spend is pressed!
    val formattedRemainingAmount = remember(safeToSpendRM) {
        if (safeToSpendRM < 0) {
            String.format(Locale.US, "-RM %,.2f", kotlin.math.abs(safeToSpendRM))
        } else if (safeToSpendRM % 1.0 == 0.0) {
            String.format(Locale.US, "RM %,d", safeToSpendRM.toLong())
        } else {
            String.format(Locale.US, "RM %,.2f", safeToSpendRM)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedGradientTop,
                        animatedGradientBottom,
                        animatedContainerColor
                    )
                )
            )
            .verticalScroll(scrollState)
    ) {
        // Taller & Longer Top Header Section with Dynamic Sparkle Gradient & AI Assistant Icon
        TopHeaderSection(
            safeAmountText = formattedRemainingAmount,
            scoreResult = scoreResult,
            dailyLeftRM = dailyLeftRM,
            onSettingsClick = onNavigateToSettings,
            onAiChatClick = onNavigateToAiChat,
            onSeeMoreClick = onNavigateToSeeMore
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Sheet Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = animatedContainerColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // "CAN I AFFORD IT?" Card with SPEND Button matching screen color transition
                AffordabilityCard(
                    amountText = formattedAmount,
                    isZero = cents == 0L,
                    cardBgColor = animatedCardColor,
                    onSpendClick = {
                        if (cents > 0L) {
                            onNavigateToSpendResult(cents)
                        }
                    }
                )

                // Custom Numeric Keypad with matching dynamic button background
                NumericKeypad(
                    buttonBgColor = animatedContainerColor,
                    onDigitClick = { digit ->
                        val digitValue = digit.toLongOrNull()
                        if (digitValue != null && cents < 100_000_000L) { // max RM 1,000,000.00
                            cents = cents * 10 + digitValue
                        }
                    },
                    onDeleteClick = {
                        cents /= 10
                    },
                    onClearClick = {
                        cents = 0L
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun TopHeaderSection(
    safeAmountText: String = "RM 2,650",
    scoreResult: ScoreResult = ScoreResult(100, "Healthy", 0xFF123C2A, 0xFF4ECA80, 85.48),
    dailyLeftRM: Double = 85.48,
    onSettingsClick: () -> Unit = {},
    onAiChatClick: () -> Unit = {},
    onSeeMoreClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Decorative Sparkle Stars Background Effect
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopStart)
                .padding(start = 50.dp, top = 28.dp)
        )
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = AccentTeal.copy(alpha = 0.3f),
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopEnd)
                .padding(end = 60.dp, top = 48.dp)
        )
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.22f),
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp), // Longer, spacious top padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Icon Action Bar (Hamburger Menu Icon & AI Assistant Penguin Icon)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu / Settings",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Top Right AI Assistant Penguin Mascot Button 🐧
                IconButton(onClick = onAiChatClick) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐧", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Spending Safe Label & Big Amount
            Text(
                text = "Safe to spend until end of month.",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.88f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = safeAmountText,
                style = TextStyle(
                    fontSize = 50.sp, // Larger, prominent amount
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Debt Score & Daily Left Display below Big Safe Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Debt score ${scoreResult.score} • ${scoreResult.statusLabel}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(scoreResult.textHex)
                    )
                )
                Text(
                    text = "•",
                    style = TextStyle(fontSize = 14.sp, color = TextSecondary)
                )
                Text(
                    text = String.format(Locale.US, "Daily Left: RM %,.2f/day", dailyLeftRM),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent "SEE MORE" Button
            Surface(
                modifier = Modifier.clickable { onSeeMoreClick() },
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SEE MORE",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            color = Color.White
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "See More Details",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AffordabilityCard(
    amountText: String,
    isZero: Boolean,
    cardBgColor: Color = Color(0xFF1B3325),
    onSpendClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = cardBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "CAN I AFFORD IT?",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter the amount you want to spend.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input field + SPEND Button Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(InputFieldDark)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RM ",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                )

                BasicTextField(
                    value = amountText,
                    onValueChange = {},
                    readOnly = true, // Keypad controls input
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isZero) TextSecondary else Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // SPEND Action Button (Disabled when 0.00)
                val isSpendEnabled = !isZero
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSpendEnabled) AccentTeal else AccentTeal.copy(alpha = 0.35f))
                        .clickable(enabled = isSpendEnabled) { onSpendClick() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SPEND",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpendEnabled) Color(0xFF101E17) else Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NumericKeypad(
    buttonBgColor: Color = DarkGreenContainer,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keypadGrid = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("CLEAR", "0", "DEL")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keypadGrid.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { symbol ->
                    KeypadButton(
                        symbol = symbol,
                        buttonBgColor = buttonBgColor,
                        onClick = {
                            when (symbol) {
                                "DEL" -> onDeleteClick()
                                "CLEAR" -> onClearClick()
                                else -> onDigitClick(symbol)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    symbol: String,
    buttonBgColor: Color = DarkGreenContainer,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "KeypadButtonScale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(buttonBgColor)
            .clickable {
                isPressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(100)
                isPressed = false
            }
        }

        when (symbol) {
            "DEL" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            "CLEAR" -> {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear All",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            else -> {
                Text(
                    text = symbol,
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun PredictedExpensesCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1B3325)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: "PREDICTED EXPENSES" & "See all"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PREDICTED EXPENSES",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                )

                Text(
                    text = "See all",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentTeal
                    ),
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amounts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "RM 1,300",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "left to pay",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "March total ",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    )
                    Text(
                        text = "RM 3,700",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-tone Expense Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxWidth()
                        .background(AccentPink)
                )
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxWidth()
                        .background(AccentTeal)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "Next payment" Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.12f)
                )
                Text(
                    text = "Next payment",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = TextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.12f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Payment Item Row (Mortgage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mortgage",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Recurring Icon Badge (Yellow)
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(RecurringGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recurring",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = "RM 2,400",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Monthly • 27/04/2023",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    CredityTheme {
        HomeScreen()
    }
}
