package com.example.credity

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private val DarkBackground = Color(0xFF121418)
private val CardSurfaceDark = Color(0xFF1B1D22)
private val TextSecondary = Color(0xFF9E9EA5)
private val AccentTeal = Color(0xFF6BC8A6)
private val UserBubbleBg = Color(0xFF2C4C38)

data class ChatMessage(
    val sender: String, // "AI" or "User"
    val text: String
)

data class FixedQuestion(
    val id: Int,
    val text: String,
    val options: List<QuizOption>
)

@Composable
fun AiChatScreen(
    profile: UserFinancialProfile = UserFinancialProfile(),
    onUpdateProfile: (UserFinancialProfile) -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GrafilabAiService() }
    val firebaseRepo = remember { FirebaseRepository() }
    val googleSheetsRepo = remember { GoogleSheetsRepository() }

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    var userInputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var liveTransactionsCsv by remember { mutableStateOf("") }

    // Dynamic suggested questions shuffled every time the screen is opened
    val suggestedQuestions = remember {
        listOf(
            listOf(
                "💡 How will my current spending affect credit card interest?",
                "📈 How do I raise my credit score by 50 points?",
                "🛡️ What is my forecasted risk of credit card debt?"
            ),
            listOf(
                "💳 How can I avoid paying 15% APR credit card interest?",
                "📊 What is my ideal daily safe spending limit?",
                "🚀 What are the top 3 steps to pay off debt faster?"
            ),
            listOf(
                "🚨 Will my current expenses trigger a credit score drop?",
                "💰 How much should I keep in my emergency cash buffer?",
                "⚡ What is my best strategy to manage upcoming bills?"
            )
        ).shuffled().first()
    }

    // Dynamic profiling questions generated / shuffled on open
    val fixedQuestions = remember {
        val questionSets = listOf(
            listOf(
                FixedQuestion(
                    id = 1,
                    text = "🐧 **Penny's Question 1 of 3:** How do you handle your monthly credit card statement payments?",
                    options = listOf(
                        QuizOption("A", "I pay the full statement balance every month"),
                        QuizOption("B", "I pay only the minimum payment required"),
                        QuizOption("C", "I pay more than minimum but carry a balance"),
                        QuizOption("D", "I occasionally delay or miss payments")
                    )
                ),
                FixedQuestion(
                    id = 2,
                    text = "🐧 **Penny's Question 2 of 3:** What is your primary financial goal right now?",
                    options = listOf(
                        QuizOption("A", "Prevent credit card interest & clear debt"),
                        QuizOption("B", "Boost my credit score by 50+ points"),
                        QuizOption("C", "Build an emergency cash savings fund"),
                        QuizOption("D", "Control daily impulse spending")
                    )
                ),
                FixedQuestion(
                    id = 3,
                    text = "🐧 **Penny's Question 3 of 3:** How do you handle unexpected expenses during the month?",
                    options = listOf(
                        QuizOption("A", "Use my daily safe-to-spend cash buffer"),
                        QuizOption("B", "Charge it to my credit card"),
                        QuizOption("C", "Dip into my emergency savings"),
                        QuizOption("D", "Cut back on non-essential spending")
                    )
                )
            ),
            listOf(
                FixedQuestion(
                    id = 1,
                    text = "🐧 **Penny's Question 1 of 3:** How frequently do you use your credit card for daily purchases?",
                    options = listOf(
                        QuizOption("A", "Every day for cash back / rewards"),
                        QuizOption("B", "Only for big essential purchases"),
                        QuizOption("C", "When my debit / cash buffer runs low"),
                        QuizOption("D", "Rarely / emergency use only")
                    )
                ),
                FixedQuestion(
                    id = 2,
                    text = "🐧 **Penny's Question 2 of 3:** How much of your monthly budget goes toward credit card payments?",
                    options = listOf(
                        QuizOption("A", "Less than 10% of my income"),
                        QuizOption("B", "Between 10% and 30% of my income"),
                        QuizOption("C", "More than 30% of my income"),
                        QuizOption("D", "Unsure / varies each month")
                    )
                ),
                FixedQuestion(
                    id = 3,
                    text = "🐧 **Penny's Question 3 of 3:** What is your biggest obstacle to saving money each month?",
                    options = listOf(
                        QuizOption("A", "High fixed bills & rent costs"),
                        QuizOption("B", "Unplanned dining & retail spending"),
                        QuizOption("C", "Interest charges on existing balances"),
                        QuizOption("D", "Unexpected emergencies")
                    )
                )
            )
        )
        questionSets.shuffled().first()
    }

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val userProfilingAnswers = remember { mutableStateListOf<String>() }
    var isQuestionnaireComplete by remember { mutableStateOf(false) }

    fun sendCustomUserPrompt(promptText: String) {
        if (promptText.isBlank() || isLoading) return

        chatMessages.add(ChatMessage("User", promptText))
        userInputText = ""
        isLoading = true

        coroutineScope.launch {
            if (liveTransactionsCsv.isBlank()) {
                liveTransactionsCsv = googleSheetsRepo.fetchLiveTransactionHistory().getOrDefault("")
            }

            val historyPairs = chatMessages.map { Pair(it.sender, it.text) }
            val result = aiService.getPersonalizedAiAdvice(
                userMessage = promptText,
                profile = profile.copy(quizAnswers = userProfilingAnswers),
                liveTransactionsCsv = liveTransactionsCsv,
                chatHistory = historyPairs
            )
            isLoading = false

            result.onSuccess { response ->
                chatMessages.add(ChatMessage("AI", response.adviceText))

                response.extractedInsight?.let { newInsight ->
                    val updatedInsights = (profile.learnedInsights + newInsight).distinct()
                    val updatedProfile = profile.copy(
                        learnedInsights = updatedInsights,
                        quizAnswers = userProfilingAnswers
                    )
                    onUpdateProfile(updatedProfile)
                    firebaseRepo.saveUserProfile(updatedProfile)
                }
            }
        }
    }

    fun answerProfilingQuestion(selectedOpt: QuizOption) {
        val answerRecord = "Q${currentQuestionIndex + 1}: ${selectedOpt.text}"
        userProfilingAnswers.add(answerRecord)
        chatMessages.add(ChatMessage("User", selectedOpt.text))

        if (currentQuestionIndex < fixedQuestions.size - 1) {
            currentQuestionIndex++
            chatMessages.add(ChatMessage("AI", fixedQuestions[currentQuestionIndex].text))
        } else {
            isQuestionnaireComplete = true
            chatMessages.add(ChatMessage("AI", "🐧 **Penny's Profiling Complete!** Analyzing your responses, live transactions, and RM ${profile.incomePerMonth} income to build your thorough debt-prevention forecast..."))
            
            val combinedAnswersPrompt = "Here are my profiling responses: ${userProfilingAnswers.joinToString("; ")}. " +
                    "Please evaluate my RM ${profile.incomePerMonth} income, RM ${profile.totalExpensesRM} fixed expenses, " +
                    "and RM ${profile.totalSpentThisMonthRM} spent this month. Give me a thorough, complete point-form forecast to prevent credit card debt and maximize my credit score."
            
            sendCustomUserPrompt(combinedAnswersPrompt)
        }
    }

    // Display Question 1 directly on launch (No "Hello" greeting)
    LaunchedEffect(Unit) {
        if (chatMessages.isEmpty()) {
            chatMessages.add(ChatMessage("AI", fixedQuestions[0].text))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Navigation Bar featuring Penny the Penguin Mascot 🐧
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentTeal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🐧",
                    style = TextStyle(fontSize = 20.sp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Penny the Penguin",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Your Penguin Financial Advisor • Live Forecasts",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        // Conversation Feed with Typewriter Animation for AI
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chatMessages) { message ->
                if (message.sender == "User") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = UserBubbleBg,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = parseMarkdownToAnnotatedString(message.text),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    lineHeight = 21.sp
                                ),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardSurfaceDark,
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                TypewriterText(
                                    text = message.text,
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Fixed Questionnaire Option Cards
            if (!isQuestionnaireComplete && currentQuestionIndex < fixedQuestions.size) {
                item {
                    val q = fixedQuestions[currentQuestionIndex]
                    Text(
                        text = "SELECT AN ANSWER:",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentTeal
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        q.options.forEach { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { answerProfilingQuestion(option) },
                                shape = RoundedCornerShape(14.dp),
                                color = CardSurfaceDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(AccentTeal),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option.optionId,
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF101E17)
                                            )
                                        )
                                    }

                                    Text(
                                        text = option.text,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentTeal,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Penny the Penguin is calculating financial risk...",
                            style = TextStyle(fontSize = 12.sp, color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Suggested Question Chips & Prompt Bar (Shown after Questionnaire)
        if (isQuestionnaireComplete) {
            if (!isLoading) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedQuestions) { question ->
                        Surface(
                            modifier = Modifier.clickable { sendCustomUserPrompt(question) },
                            shape = RoundedCornerShape(16.dp),
                            color = CardSurfaceDark
                        ) {
                            Text(
                                text = question,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentTeal
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardSurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = userInputText,
                        onValueChange = { userInputText = it },
                        placeholder = { Text("Ask Penny the Penguin for financial advice, credit tips...", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedPlaceholderColor = TextSecondary,
                            unfocusedPlaceholderColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { sendCustomUserPrompt(userInputText) },
                        enabled = userInputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (userInputText.isNotBlank()) AccentTeal else Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Prompt",
                            tint = if (userInputText.isNotBlank()) Color(0xFF101E17) else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AiChatScreenPreview() {
    CredityTheme {
        AiChatScreen(onBackClick = {})
    }
}
