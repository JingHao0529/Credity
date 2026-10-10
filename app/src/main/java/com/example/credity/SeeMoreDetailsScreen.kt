package com.example.credity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.Locale

private val DarkBackground = Color(0xFF121418)
private val CardSurfaceDark = Color(0xFF1B1D22)
private val TextSecondary = Color(0xFF9E9EA5)
private val AccentTeal = Color(0xFF6BC8A6)
private val SafeGreenText = Color(0xFF4ECA80)
private val ErrorRed = Color(0xFFE55353)

@Composable
fun SeeMoreDetailsScreen(
    incomeRM: Double,
    expensesList: List<ExpenseItem>,
    totalSpentThisMonthRM: Double,
    onUpdateIncome: (Double) -> Unit,
    onUpdateExpenseAmount: (String, Double) -> Unit,
    onAddExpense: (String, Double, Int) -> Unit,
    onDeleteExpense: (String) -> Unit = {},
    onTogglePaid: (String, Boolean) -> Unit,
    onClearTotalSpentThisMonth: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val totalExpensesRM = expensesList.sumOf { it.amountRM }
    val remainingSafeToSpendRM = maxOf(0.0, incomeRM - totalExpensesRM - totalSpentThisMonthRM)

    // Modal Numpad States
    var showEditIncomeModal by remember { mutableStateOf(false) }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Budget & Expenses Overview",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Remaining Safe to Spend (Middle Large Header)
        Text(
            text = "Remaining Safe to Spend",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = String.format(Locale.US, "RM %,.2f", remainingSafeToSpendRM),
            style = TextStyle(
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = SafeGreenText
            )
        )

        Text(
            text = "(Income - Expenses - Spent This Month)",
            style = TextStyle(
                fontSize = 12.sp,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Income Card with Edit Icon
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = CardSurfaceDark
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Income",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "RM %,.2f", incomeRM),
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                IconButton(
                    onClick = { showEditIncomeModal = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Income",
                        tint = AccentTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Monthly Expenses List Header & Add Expense Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY EXPENSES",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
            )

            TextButton(onClick = { showAddExpenseModal = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Expense",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Expense Cards List with Checkbox Animation & Smooth Item Removal
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            expensesList.forEach { expense ->
                AnimatedVisibility(
                    visible = !expense.isPaid,
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = CardSurfaceDark
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = expense.isPaid,
                                        onCheckedChange = { checked ->
                                            onTogglePaid(expense.id, checked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentTeal,
                                            uncheckedColor = TextSecondary
                                        )
                                    )

                                    Column {
                                        Text(
                                            text = expense.name,
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                        Text(
                                            text = expense.dueDateText,
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(Locale.US, "RM %,.2f", expense.amountRM),
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (expense.isPaid) TextSecondary else Color.White
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            editingExpenseId = expense.id
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Expense Amount",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Countdown Progress Bar (based on 30 days)
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
                                        .background(if (expense.isPaid) TextSecondary else AccentTeal)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Total Monthly Expenses Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Monthly Expenses",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = String.format(Locale.US, "RM %,.2f", totalExpensesRM),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                )
            }
        }

        // Total Spent This Month (Cumulative "Spend Anyway") Card with Delete Icon
        if (totalSpentThisMonthRM > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ErrorRed.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Spent This Month",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Cumulative 'Spend Anyway' Purchases",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "RM %,.2f", totalSpentThisMonthRM),
                            style = TextStyle(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = onClearTotalSpentThisMonth) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Total Spent Data",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Modal Numpad Dialog for Editing Income (Income >= Expenses Validation)
    if (showEditIncomeModal) {
        var tempCents by remember { mutableLongStateOf((incomeRM * 100).toLong()) }
        val formatted = String.format(Locale.US, "RM %,.2f", tempCents / 100.0)

        AlertDialog(
            onDismissRequest = {
                showEditIncomeModal = false
                validationError = null
            },
            title = { Text("Set Monthly Income", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatted,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    )

                    validationError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = TextStyle(fontSize = 12.sp, color = ErrorRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NumericKeypad(
                        onDigitClick = { digit ->
                            validationError = null
                            val digitVal = digit.toLongOrNull()
                            if (digitVal != null && tempCents < 100_000_000L) {
                                tempCents = tempCents * 10 + digitVal
                            }
                        },
                        onDeleteClick = {
                            validationError = null
                            tempCents /= 10
                        },
                        onClearClick = {
                            validationError = null
                            tempCents = 0L
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newIncome = tempCents / 100.0
                        if (newIncome < totalExpensesRM) {
                            validationError = String.format(Locale.US, "Income cannot be less than total expenses (RM %,.2f).", totalExpensesRM)
                        } else {
                            onUpdateIncome(newIncome)
                            showEditIncomeModal = false
                            validationError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Save Income", color = Color(0xFF101E17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditIncomeModal = false
                    validationError = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurfaceDark
        )
    }

    // Modal Numpad Dialog for Editing Expense Amount WITH Delete Option
    editingExpenseId?.let { expId ->
        val existingExp = expensesList.find { it.id == expId }
        var tempCents by remember(expId) {
            mutableLongStateOf(((existingExp?.amountRM ?: 0.0) * 100).toLong())
        }
        val formatted = String.format(Locale.US, "RM %,.2f", tempCents / 100.0)

        AlertDialog(
            onDismissRequest = {
                editingExpenseId = null
                validationError = null
            },
            title = { Text("Edit ${existingExp?.name ?: "Expense"}", color = Color.White) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatted,
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    )

                    validationError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = err,
                            style = TextStyle(fontSize = 12.sp, color = ErrorRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NumericKeypad(
                        onDigitClick = { digit ->
                            validationError = null
                            val digitVal = digit.toLongOrNull()
                            if (digitVal != null && tempCents < 100_000_000L) {
                                tempCents = tempCents * 10 + digitVal
                            }
                        },
                        onDeleteClick = {
                            validationError = null
                            tempCents /= 10
                        },
                        onClearClick = {
                            validationError = null
                            tempCents = 0L
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = tempCents / 100.0
                        val otherExpenses = expensesList.filter { it.id != expId }.sumOf { it.amountRM }
                        if (otherExpenses + newAmount > incomeRM) {
                            validationError = String.format(Locale.US, "Total expenses cannot exceed income (RM %,.2f).", incomeRM)
                        } else {
                            onUpdateExpenseAmount(expId, newAmount)
                            editingExpenseId = null
                            validationError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Save Amount", color = Color(0xFF101E17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Expense Button
                    TextButton(
                        onClick = {
                            onDeleteExpense(expId)
                            editingExpenseId = null
                            validationError = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Expense",
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = {
                        editingExpenseId = null
                        validationError = null
                    }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            },
            containerColor = CardSurfaceDark
        )
    }

    // Modal Numpad Dialog for Adding New Expense (Expenses <= Income Validation)
    if (showAddExpenseModal) {
        var expName by remember { mutableStateOf("") }
        var daysText by remember { mutableStateOf("15") }
        var tempCents by remember { mutableLongStateOf(0L) }
        val formatted = String.format(Locale.US, "RM %,.2f", tempCents / 100.0)

        AlertDialog(
            onDismissRequest = {
                showAddExpenseModal = false
                validationError = null
            },
            title = { Text("Add New Expense", color = Color.White) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = expName,
                        onValueChange = { expName = it },
                        label = { Text("Expense Name (e.g. Gym, Internet)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { daysText = it },
                        label = { Text("Days Left Until Due Date") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentTeal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Amount: $formatted",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    )

                    validationError?.let { err ->
                        Text(
                            text = err,
                            style = TextStyle(fontSize = 12.sp, color = ErrorRed)
                        )
                    }

                    NumericKeypad(
                        onDigitClick = { digit ->
                            validationError = null
                            val digitVal = digit.toLongOrNull()
                            if (digitVal != null && tempCents < 100_000_000L) {
                                tempCents = tempCents * 10 + digitVal
                            }
                        },
                        onDeleteClick = {
                            validationError = null
                            tempCents /= 10
                        },
                        onClearClick = {
                            validationError = null
                            tempCents = 0L
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountDouble = tempCents / 100.0
                        val daysInt = daysText.toIntOrNull() ?: 15
                        if (expName.isBlank()) {
                            validationError = "Please enter an expense name."
                        } else if (totalExpensesRM + amountDouble > incomeRM) {
                            validationError = String.format(Locale.US, "Total expenses cannot exceed income (RM %,.2f).", incomeRM)
                        } else {
                            onAddExpense(expName, amountDouble, daysInt)
                            showAddExpenseModal = false
                            validationError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Add Expense", color = Color(0xFF101E17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddExpenseModal = false
                    validationError = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardSurfaceDark
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SeeMoreDetailsScreenPreview() {
    CredityTheme {
        SeeMoreDetailsScreen(
            incomeRM = 5000.0,
            expensesList = listOf(
                ExpenseItem("1", "Rent", 1450.0, 12, "12 days left", true, false),
                ExpenseItem("2", "Utilities", 300.0, 5, "5 days left", true, false),
                ExpenseItem("3", "Mortgage", 2400.0, 27, "27 days left", true, false),
                ExpenseItem("4", "Groceries", 600.0, 2, "2 days left", false, false)
            ),
            totalSpentThisMonthRM = 84.50,
            onUpdateIncome = {},
            onUpdateExpenseAmount = { _, _ -> },
            onAddExpense = { _, _, _ -> },
            onDeleteExpense = {},
            onTogglePaid = { _, _ -> },
            onClearTotalSpentThisMonth = {},
            onBackClick = {}
        )
    }
}
