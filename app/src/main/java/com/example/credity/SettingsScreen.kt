package com.example.credity

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
private val TextSecondary = Color(0xFF9E9EA5)
private val AccentTeal = Color(0xFF6BC8A6)

@Composable
fun SettingsScreen(
    incomeRM: Double = 5000.0,
    totalExpensesRM: Double = 2350.0,
    safeToSpendRM: Double = 2650.0,
    onApplyExtractedStatement: (Double, List<ExpenseItem>) -> Unit = { _, _ -> },
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val firebaseRepo = remember { FirebaseRepository() }
    val aiService = remember { GrafilabAiService() }

    var syncStatusText by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Statement Import States
    var statementInputText by remember { mutableStateOf("") }
    var isAnalyzingStatement by remember { mutableStateOf(false) }
    var statementResult by remember { mutableStateOf<StatementAnalysisResult?>(null) }

    // File / PDF Statement Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val textContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (textContent.isNotBlank()) {
                    statementInputText = textContent
                    isAnalyzingStatement = true
                    coroutineScope.launch {
                        val result = aiService.parseFinancialStatement(textContent)
                        isAnalyzingStatement = false
                        statementResult = result.getOrNull()
                    }
                }
            } catch (e: Exception) {
                syncStatusText = "Error reading statement file: ${e.localizedMessage}"
            }
        }
    }

    val sampleStatementText = """
        [BANK TRANSACTION STATEMENT - MARCH 2024]
        01/03/2024 - CRED: Salary Deposit +RM 5,500.00
        02/03/2024 - DEB: Monthly Apartment Rent -RM 1,500.00
        03/03/2024 - DEB: Electric & Water Bill -RM 350.00
        05/03/2024 - DEB: Apple Store MacBook Pro -RM 8,500.00 (1-time purchase)
        08/03/2024 - DEB: Unifi Fiber Home Internet -RM 150.00
        12/03/2024 - DEB: Grocery Supermarket -RM 600.00
        15/03/2024 - DEB: Switch Authorized iPhone 15 Pro -RM 4,800.00 (1-time purchase)
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccentTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AM",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF101E17)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Alex Morgan",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "alex.morgan@example.com",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // NEW FEATURE: FINANCIAL STATEMENT IMPORT & AI NOISE FILTERING
        Text(
            text = "FINANCIAL STATEMENT IMPORT & AI PARSER",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Statement",
                            tint = Color(0xFF101E17),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Import Statement History (PDF/CSV)",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Penny AI removes 1-time laptop/phone noise for accurate forecasts",
                            style = TextStyle(fontSize = 11.sp, color = TextSecondary)
                        )
                    }
                }

                OutlinedTextField(
                    value = statementInputText,
                    onValueChange = { statementInputText = it },
                    placeholder = { Text("Paste bank transactions CSV / PDF statement text here...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedPlaceholderColor = TextSecondary,
                        unfocusedPlaceholderColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload PDF",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload PDF", fontSize = 10.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { statementInputText = sampleStatementText },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sample PDF", fontSize = 10.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            if (statementInputText.isBlank()) return@Button
                            isAnalyzingStatement = true
                            coroutineScope.launch {
                                val result = aiService.parseFinancialStatement(statementInputText)
                                isAnalyzingStatement = false
                                statementResult = result.getOrNull()
                            }
                        },
                        enabled = statementInputText.isNotBlank() && !isAnalyzingStatement,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        if (isAnalyzingStatement) {
                            CircularProgressIndicator(color = Color(0xFF101E17), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Analyze Penny 🐧", fontSize = 10.sp, color = Color(0xFF101E17), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // AI ANALYSIS RESULT CARD
                statementResult?.let { result ->
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = parseMarkdownToAnnotatedString(result.summaryReport),
                            style = TextStyle(fontSize = 13.sp, color = Color.White, lineHeight = 19.sp)
                        )

                        if (result.filteredNoiseItems.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.05f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Filtered 1-Time Purchases (Noise Removed):",
                                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    result.filteredNoiseItems.forEach { noise ->
                                        Text(text = "• $noise", style = TextStyle(fontSize = 11.sp, color = TextSecondary))
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onApplyExtractedStatement(result.extractedIncomeRM, result.extractedExpenses)
                                syncStatusText = "Applied RM ${result.extractedIncomeRM} Income & ${result.extractedExpenses.size} Expenses!"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF101E17), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply AI Extracted Budget To App", color = Color(0xFF101E17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FIREBASE DATABASE SECTION
        Text(
            text = "FIREBASE CLOUD DATABASE",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stored Data Fields:",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = String.format(
                        Locale.US,
                        "• Name: Alex Morgan\n" +
                                "• Age Range: 25-34\n" +
                                "• Gender: Male\n" +
                                "• Credit Card Interest Rate: 24.0%%\n" +
                                "• Income per Month: RM %,.2f\n" +
                                "• Predicted Expenses per Month: RM %,.2f\n" +
                                "• Safe to Spend Value: RM %,.2f",
                        incomeRM,
                        totalExpensesRM,
                        safeToSpendRM
                    ),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        isSaving = true
                        syncStatusText = "Writing data to Firebase..."
                        coroutineScope.launch {
                            val profile = UserFinancialProfile(
                                incomePerMonth = incomeRM
                            )
                            val result = firebaseRepo.saveUserProfile(profile)
                            isSaving = false
                            syncStatusText = result.fold(
                                onSuccess = { "Success:\n$it" },
                                onFailure = { "Error: ${it.localizedMessage}" }
                            )
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Save to Firebase",
                        tint = Color(0xFF101E17),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Profile to Firebase Database",
                        color = Color(0xFF101E17),
                        fontWeight = FontWeight.Bold
                    )
                }

                syncStatusText?.let { status ->
                    Text(
                        text = status,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (status.startsWith("Error")) Color.Red else AccentTeal
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preferences Group
        Text(
            text = "PREFERENCES",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account Details",
                    subtitle = "Personal information & security"
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsItem(
                    icon = Icons.Default.CreditCard,
                    title = "Currency",
                    subtitle = "Malaysian Ringgit (RM)",
                    valueText = "RM"
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Monthly Safe Budget",
                    subtitle = String.format(Locale.US, "RM %,.2f", safeToSpendRM),
                    valueText = String.format(Locale.US, "RM %,d", safeToSpendRM.toLong())
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Group
        Text(
            text = "ABOUT",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            ),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "Credity v1.0.0",
                    valueText = "1.0.0"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueText: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valueText != null) {
                Text(
                    text = valueText,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentTeal
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    CredityTheme {
        SettingsScreen(onBackClick = {})
    }
}
