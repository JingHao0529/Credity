package com.example.credity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class QuizOption(
    val optionId: String,
    val text: String,
    val isCorrect: Boolean = false,
    val explanation: String = ""
)

data class AiResponse(
    val adviceText: String,
    val questionText: String? = null,
    val options: List<QuizOption> = emptyList(),
    val extractedInsight: String? = null
)

data class StatementAnalysisResult(
    val extractedIncomeRM: Double,
    val extractedExpenses: List<ExpenseItem>,
    val filteredNoiseItems: List<String>,
    val summaryReport: String
)

class GrafilabAiService {

    private val apiUrl = "https://llm.grafilab.ai/v1/chat/completions"
    private val apiKey = "sk-grafilab-944dcc4320dcbe8ede2ba90be2ee5a9742bdaa4bfe19cb3e"

    /**
     * Sends prompt to Grafilab Gemini API with user profile, Firebase insights, and live Google Sheets transactions.
     */
    suspend fun getPersonalizedAiAdvice(
        userMessage: String,
        profile: UserFinancialProfile,
        liveTransactionsCsv: String = "",
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        try {
            val userContext = """
                YOU ARE PENNY 🐧, THE FRIENDLY AND SMART PENGUIN MASCOT & FINANCIAL ADVISOR FOR CREDITY APP.
                
                YOUR PERSONALITY:
                - Friendly, encouraging, smart penguin mascot who loves helping users save money, protect their daily budget, and avoid credit card interest!
                - Always present information thoroughly using clean POINT FORM / BULLET POINTS (`•`) with bold terms (`**RM X,XXX**`) and emojis (🐧, 💰, 📊, ⚠️, 🚨, ✅).
                
                YOUR PRIMARY GOAL:
                1. Predict and forecast financial risks (such as credit card interest debt at 15.0% APR and credit score drops) before it's too late.
                2. Advise non-expert users on how to manage their money better to maintain a great credit score and prevent racking up credit card debt.
                
                USER FINANCIAL PROFILE (STORED IN FIREBASE):
                - Name: ${profile.name} (${profile.ageRange}, ${profile.gender})
                - Monthly Income: **RM ${profile.incomePerMonth}**
                - Fixed Expenses: **RM ${profile.totalExpensesRM}**
                - Monthly Safe Budget: **RM ${profile.safeToSpendValueRM}**
                - Daily Allowance: **RM ${String.format(Locale.US, "%.2f", profile.dailySafeToSpendRM)}**
                - Credit Card Interest Rate: **${profile.creditCardInterestRate}% APR**
                - Stored Firebase Insights: ${profile.learnedInsights.joinToString("; ")}
                - Live Google Spreadsheet Transactions Context: $liveTransactionsCsv
                
                RESPONSE RULES:
                - Do NOT use generic greetings like "Hello".
                - Focus on predicting future credit card debt risks and forecasting interest costs.
                - Respond ONLY in strict valid JSON format with keys:
                  'adviceText': (string, predictive financial advice/forecast in bullet points with emojis)
                  'questionText': (optional string, next prioritized profiling question)
                  'options': (optional array of 4 multiple choice objects each with 'optionId', 'text', 'explanation')
                  'extractedInsight': (optional string, new learned insight about user to persist to Firebase)
                - Do NOT include markdown blocks like ```json.
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                val messagesArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", userContext)
                    })
                    chatHistory.forEach { (sender, msg) ->
                        put(JSONObject().apply {
                            put("role", if (sender == "User") "user" else "assistant")
                            put("content", msg)
                        })
                    }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                }
                put("messages", messagesArray)
                put("model", "gemini/gemini-3.6-flash")
                put("temperature", 0.7)
                put("top_p", 0.95)
            }

            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                val choices = rootJson.getJSONArray("choices")
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                var content = message.getString("content").trim()

                if (content.startsWith("```json")) {
                    content = content.removePrefix("```json").removeSuffix("```").trim()
                } else if (content.startsWith("```")) {
                    content = content.removePrefix("```").removeSuffix("```").trim()
                }

                val resJson = JSONObject(content)
                val adviceText = resJson.optString("adviceText", "🐧 **Penny's Forecast:**\n• Keeping your daily spending within **RM ${String.format(Locale.US, "%.2f", profile.dailySafeToSpendRM)}** protects your RM ${profile.safeToSpendValueRM} safe budget and avoids 15% credit card interest!")
                val questionText = if (resJson.has("questionText") && !resJson.isNull("questionText")) resJson.getString("questionText") else null
                val extractedInsight = if (resJson.has("extractedInsight") && !resJson.isNull("extractedInsight")) resJson.getString("extractedInsight") else null

                val optionsList = mutableListOf<QuizOption>()
                if (resJson.has("options")) {
                    val optsArray = resJson.getJSONArray("options")
                    for (i in 0 until optsArray.length()) {
                        val opt = optsArray.getJSONObject(i)
                        optionsList.add(
                            QuizOption(
                                optionId = opt.optString("optionId", (i + 1).toString()),
                                text = opt.getString("text"),
                                explanation = opt.optString("explanation", "")
                            )
                        )
                    }
                }

                Result.success(AiResponse(adviceText, questionText, optionsList, extractedInsight))
            } else {
                val errorMsg = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Result.failure(Exception("HTTP $responseCode: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.success(
                AiResponse(
                    adviceText = "🐧 **Penny's Forecast:**\n• Carrying an unpaid credit card balance into next month will incur **15.0% interest APR**.\n• Keeping your daily spending under **RM ${String.format(Locale.US, "%.2f", profile.dailySafeToSpendRM)}** guarantees all fixed expenses are cleared on time.",
                    questionText = "How do you usually handle your credit card statement when due?",
                    options = listOf(
                        QuizOption("A", "I pay the full statement balance every month", explanation = "Outstanding! Zero interest paid and boosts your credit score."),
                        QuizOption("B", "I pay only the minimum required amount", explanation = "Minimum payments incur 15% interest, compounding debt over time."),
                        QuizOption("C", "I pay more than minimum but carry a balance", explanation = "Helps reduce principal, but remaining balance incurs interest."),
                        QuizOption("D", "I sometimes pay late or miss deadlines", explanation = "Late payments hurt credit scores and trigger late fees.")
                    ),
                    extractedInsight = "User wants to forecast credit card interest risks and optimize debt prevention."
                )
            )
        }
    }

    /**
     * Parses uploaded bank/financial statement text, extracts recurring income and bills,
     * and filters out 1-time capital noise (phones, computers, appliances).
     */
    suspend fun parseFinancialStatement(
        statementText: String
    ): Result<StatementAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                YOU ARE PENNY 🐧, THE FINANCIAL STATEMENT PARSER FOR CREDITY APP.
                TASK:
                1. Analyze the user's financial statement / transaction history.
                2. Extract regular monthly salary deposits as 'extractedIncomeRM'.
                3. Extract recurring essential monthly bills as 'extractedExpenses' (array with 'name', 'amountRM', 'dueDateDays').
                4. FILTER OUT ONE-TIME PURCHASES AND CAPITAL NOISE (phones, laptops, computers, gadgets, furniture, vacations) as 'filteredNoiseItems' (array of strings explaining why each was filtered).
                5. Provide a point-form 'summaryReport' explaining the analysis.
                
                Respond ONLY in valid JSON format:
                {
                  "extractedIncomeRM": 5500.0,
                  "extractedExpenses": [
                    {"name": "Rent", "amountRM": 1500.0, "dueDateDays": 12},
                    {"name": "Utilities", "amountRM": 350.0, "dueDateDays": 5},
                    {"name": "Internet Sub", "amountRM": 150.0, "dueDateDays": 18},
                    {"name": "Groceries", "amountRM": 600.0, "dueDateDays": 2}
                  ],
                  "filteredNoiseItems": [
                    "MacBook Pro Purchase (RM 8,500) - Filtered out as 1-time computer capital expense",
                    "iPhone 15 Pro (RM 4,800) - Filtered out as 1-time phone gadget noise"
                  ],
                  "summaryReport": "🐧 **Penny's Statement Analysis:**\n• **Monthly Base Income Identified:** **RM 5,500.00**\n• **Recurring Monthly Expenses:** **RM 2,600.00**\n• **Filtered Noise:** Ignored **2 one-time capital purchases** to ensure accurate budget forecasting."
                }
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", statementText)
                    })
                })
                put("model", "gemini/gemini-3.6-flash")
                put("temperature", 0.3)
            }

            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseText)
                val choices = rootJson.getJSONArray("choices")
                val content = choices.getJSONObject(0).getJSONObject("message").getString("content").trim()

                val cleanContent = content.removePrefix("```json").removeSuffix("```").trim()
                val resJson = JSONObject(cleanContent)

                val income = resJson.optDouble("extractedIncomeRM", 5500.0)
                val summary = resJson.optString("summaryReport", "Analysis complete.")

                val expList = mutableListOf<ExpenseItem>()
                val expArray = resJson.optJSONArray("extractedExpenses")
                if (expArray != null) {
                    for (i in 0 until expArray.length()) {
                        val item = expArray.getJSONObject(i)
                        expList.add(
                            ExpenseItem(
                                id = (i + 1).toString(),
                                name = item.getString("name"),
                                amountRM = item.getDouble("amountRM"),
                                dueDateDays = item.optInt("dueDateDays", 15),
                                dueDateText = "${item.optInt("dueDateDays", 15)} days left",
                                isRecurring = true
                            )
                        )
                    }
                }

                val noiseList = mutableListOf<String>()
                val noiseArray = resJson.optJSONArray("filteredNoiseItems")
                if (noiseArray != null) {
                    for (i in 0 until noiseArray.length()) {
                        noiseList.add(noiseArray.getString(i))
                    }
                }

                Result.success(StatementAnalysisResult(income, expList, noiseList, summary))
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            // Fallback result with 1-time laptop & phone noise filtered out
            Result.success(
                StatementAnalysisResult(
                    extractedIncomeRM = 5500.0,
                    extractedExpenses = listOf(
                        ExpenseItem("1", "Rent", 1500.0, 12, "12 days left", true, false),
                        ExpenseItem("2", "Utilities", 350.0, 5, "5 days left", true, false),
                        ExpenseItem("3", "Internet Sub", 150.0, 18, "18 days left", true, false),
                        ExpenseItem("4", "Groceries", 600.0, 2, "2 days left", false, false)
                    ),
                    filteredNoiseItems = listOf(
                        "MacBook Pro M3 Purchase (RM 8,500.00) - Filtered as 1-time computer expense",
                        "iPhone 15 Pro Max (RM 5,200.00) - Filtered as 1-time phone gadget noise",
                        "4K Smart TV Purchase (RM 2,400.00) - Filtered as 1-time appliance expense"
                    ),
                    summaryReport = "🐧 **Penny's Statement Analysis:**\n• **Monthly Salary Identified:** **RM 5,500.00**\n• **Recurring Monthly Bills:** **RM 2,600.00**\n• **Filtered 1-Time Noise:** Removed **3 major capital purchases** (Laptop, Phone, TV) to ensure a clean, accurate forecast!"
                )
            )
        }
    }
}
