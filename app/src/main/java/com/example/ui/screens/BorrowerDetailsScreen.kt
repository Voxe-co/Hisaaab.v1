package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HisaabButton
import com.example.ui.components.HisaabCard
import com.example.ui.components.HisaabTextField
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoyalBlue
import com.example.util.DummyData
import com.example.util.LoanMock
import com.example.viewmodel.BorrowerDetailsViewModel
import com.example.viewmodel.TimelineItem
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowerDetailsScreen(
    borrowerId: Long,
    viewModel: BorrowerDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Observe state flows reactively
    val borrower by viewModel.borrower.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val rawLoans by viewModel.rawLoans.collectAsStateWithLifecycle()
    val primaryRawLoan = rawLoans.firstOrNull()
    val timeline by viewModel.timeline.collectAsStateWithLifecycle()
    val borrowerPayments by viewModel.payments.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReceivePaymentSheet by remember { mutableStateOf(false) }

    // Repayment States
    var paymentAmountInput by remember { mutableStateOf("") }
    var selectedPaymentType by remember { mutableStateOf("Interest") } // Interest, Principal, Both
    var paymentDateInput by remember { mutableStateOf(System.currentTimeMillis()) }
    var paymentNoteInput by remember { mutableStateOf("") }
    var selectedLoanIndex by remember { mutableStateOf(0) }

    // Dialog state controllers
    var editNameInput by remember { mutableStateOf("") }
    var editAmountInput by remember { mutableStateOf("") }
    var editInterestRateInput by remember { mutableStateOf("") }
    var editDateInput by remember { mutableStateOf(0L) }
    var editNoteInput by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }

    // Initialize borrower selection
    LaunchedEffect(borrowerId) {
        viewModel.selectBorrower(borrowerId)
    }

    // Set up Date Picker for Loan Editing
    val datePickerDialog = remember(editDateInput) {
        val cal = Calendar.getInstance().apply { timeInMillis = if (editDateInput > 0) editDateInput else System.currentTimeMillis() }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val calSel = Calendar.getInstance()
                calSel.set(Calendar.YEAR, year)
                calSel.set(Calendar.MONTH, month)
                calSel.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                editDateInput = calSel.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(showEditDialog) {
        if (showEditDialog && primaryRawLoan != null) {
            editNameInput = primaryRawLoan.borrowerName
            editAmountInput = primaryRawLoan.amount.toString()
            editInterestRateInput = primaryRawLoan.interestRate.toString()
            editDateInput = primaryRawLoan.loanDate
            editNoteInput = primaryRawLoan.note
        }
    }

    val activeLoans = loans.filter { it.status == "ACTIVE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Borrower Ledger",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_loan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Loan",
                            tint = RoyalBlue
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_borrower_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Borrower",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (activeLoans.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showReceivePaymentSheet = true },
                    icon = { Icon(Icons.Default.Add, "Receive Repayment") },
                    text = { Text("Receive Repayment") },
                    containerColor = RoyalBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("receive_payment_fab")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        borrower?.let { b ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Borrower Premium Hero Card
                item {
                    HisaabCard(
                        backgroundColor = RoyalBlue.copy(alpha = 0.12f),
                        borderColor = RoyalBlue.copy(alpha = 0.3f),
                        elevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("borrower_hero_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(RoyalBlue)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = b.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = b.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = b.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Active Borrowed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DummyData.formatCurrency(b.totalBorrowed),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Expected Monthly Interest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DummyData.formatCurrency(loans.sumOf { it.monthlyInterestAmount }),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                )
                            }
                        }

                        if (b.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Note: ${b.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Payment History Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repayment History Ledger",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (borrowerPayments.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val firstPayment = borrowerPayments.maxByOrNull { it.paymentDate }
                                    if (firstPayment != null) {
                                        viewModel.undoLastPayment(firstPayment.loanId)
                                        try {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                        } catch (e: Exception) {}
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Undo Latest", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (borrowerPayments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No payments received yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(borrowerPayments.sortedByDescending { it.paymentDate }) { p ->
                        HisaabCard(
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            elevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = DummyData.formatDate(p.paymentDate),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (p.paymentType) {
                                                        "Interest" -> EmeraldGreen.copy(alpha = 0.15f)
                                                        "Principal" -> RoyalBlue.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = p.paymentType,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (p.paymentType) {
                                                        "Interest" -> EmeraldGreen
                                                        "Principal" -> RoyalBlue
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = DummyData.formatCurrency(p.totalPaid),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreen
                                        )
                                        IconButton(
                                            onClick = { viewModel.deletePayment(p.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Payment",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                if (p.interestPaid > 0.0 || p.principalPaid > 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        if (p.interestPaid > 0.0) {
                                            Text(
                                                text = "Int: ${DummyData.formatCurrency(p.interestPaid)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (p.principalPaid > 0.0) {
                                            Text(
                                                text = "Prin: ${DummyData.formatCurrency(p.principalPaid)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (p.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Note: ${p.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }

                // Timeline Section Header
                item {
                    Text(
                        text = "Due Dates & Contract Lifecycle Timeline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Vertical Timeline Layout
                if (timeline.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logged history for this borrower",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(timeline) { index, item ->
                        TimelineRow(
                            item = item,
                            isLast = index == timeline.size - 1
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // Receive Payment Sheet / Dialog
    if (showReceivePaymentSheet && activeLoans.isNotEmpty()) {
        val selectedLoan = activeLoans.getOrNull(selectedLoanIndex) ?: activeLoans.first()
        val remainingPrincipal = selectedLoan.amount
        val pendingInterest = selectedLoan.currentInterestDue + selectedLoan.pendingInterest
        
        val enteredAmount = paymentAmountInput.toDoubleOrNull() ?: 0.0
        
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        val interestPortion = when (selectedPaymentType) {
            "Interest" -> enteredAmount
            "Principal" -> 0.0
            else -> min(enteredAmount, pendingInterest)
        }
        val principalPortion = when (selectedPaymentType) {
            "Interest" -> 0.0
            "Principal" -> enteredAmount
            else -> max(0.0, enteredAmount - interestPortion)
        }
        
        LaunchedEffect(paymentAmountInput, selectedPaymentType, selectedLoanIndex) {
            errorMessage = when {
                paymentAmountInput.isNotBlank() && enteredAmount <= 0.0 -> {
                    "Amount must be greater than zero."
                }
                selectedPaymentType == "Interest" && enteredAmount > pendingInterest -> {
                    "Amount exceeds total pending interest of ${DummyData.formatCurrency(pendingInterest)}."
                }
                selectedPaymentType == "Principal" && enteredAmount > remainingPrincipal -> {
                    "Amount exceeds remaining loan principal of ${DummyData.formatCurrency(remainingPrincipal)}."
                }
                selectedPaymentType == "Both" && principalPortion > remainingPrincipal -> {
                    "Remaining portion of ${DummyData.formatCurrency(principalPortion)} exceeds remaining principal of ${DummyData.formatCurrency(remainingPrincipal)}."
                }
                else -> null
            }
        }
        
        val isFormValid = paymentAmountInput.isNotBlank() && enteredAmount > 0.0 && errorMessage == null

        AlertDialog(
            onDismissRequest = { showReceivePaymentSheet = false },
            title = {
                Text(
                    text = "Receive Repayment",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (activeLoans.size > 1) {
                        Text(
                            text = "Select Active Contract:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            activeLoans.forEachIndexed { idx, loan ->
                                FilterChip(
                                    selected = selectedLoanIndex == idx,
                                    onClick = { selectedLoanIndex = idx },
                                    label = { Text("₹${loan.amount.toInt()} (${loan.currentMonth})") }
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Remaining Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(DummyData.formatCurrency(remainingPrincipal), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("Total Pending Int.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(DummyData.formatCurrency(pendingInterest), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (pendingInterest > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    HisaabTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = "Repayment Amount (₹) *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("payment_amount_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Repayment Type:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Interest", "Principal", "Both").forEach { type ->
                                FilterChip(
                                    selected = selectedPaymentType == type,
                                    onClick = { selectedPaymentType = type },
                                    label = { Text(type) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (isFormValid) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldGreen.copy(alpha = 0.08f))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Repayment Allocation Split:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldGreen)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Interest Component:", style = MaterialTheme.typography.bodySmall)
                                    Text(DummyData.formatCurrency(interestPortion), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Principal Component:", style = MaterialTheme.typography.bodySmall)
                                    Text(DummyData.formatCurrency(principalPortion), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = paymentDateInput }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val sel = Calendar.getInstance()
                                    sel.set(Calendar.YEAR, year)
                                    sel.set(Calendar.MONTH, month)
                                    sel.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    paymentDateInput = sel.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text("Payment Date: ${DummyData.formatDate(paymentDateInput)}")
                    }

                    HisaabTextField(
                        value = paymentNoteInput,
                        onValueChange = { paymentNoteInput = it },
                        label = "Optional Notes",
                        modifier = Modifier.testTag("payment_notes_input")
                    )
                }
            },
            confirmButton = {
                HisaabButton(
                    text = "Confirm Repayment",
                    enabled = isFormValid,
                    onClick = {
                        viewModel.receivePayment(
                            loanId = selectedLoan.id,
                            paymentType = selectedPaymentType,
                            amount = enteredAmount,
                            interestPortion = interestPortion,
                            principalPortion = principalPortion,
                            paymentDate = paymentDateInput,
                            note = paymentNoteInput.trim()
                        )
                        showReceivePaymentSheet = false
                        paymentAmountInput = ""
                        paymentNoteInput = ""
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.testTag("confirm_payment_btn")
                )
            },
            dismissButton = {
                TextButton(onClick = { showReceivePaymentSheet = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Edit Loan Dialog Sheet
    if (showEditDialog && primaryRawLoan != null) {
        val isEditFormValid = editNameInput.isNotBlank() && 
                              (editAmountInput.toDoubleOrNull() ?: 0.0) > 0.0 && 
                              (editInterestRateInput.toDoubleOrNull() ?: 0.0) > 0.0

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Loan Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    HisaabTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = "Borrower Name *",
                        modifier = Modifier.testTag("edit_loan_name_input")
                    )
                    HisaabTextField(
                        value = editAmountInput,
                        onValueChange = { editAmountInput = it },
                        label = "Principal Amount (₹) *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("edit_loan_amount_input")
                    )
                    HisaabTextField(
                        value = editInterestRateInput,
                        onValueChange = { editInterestRateInput = it },
                        label = "Monthly Interest Rate (%) *",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.testTag("edit_loan_rate_input")
                    )
                    
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(text = "Loan Date: ${DummyData.formatDate(editDateInput)}")
                    }

                    HisaabTextField(
                        value = editNoteInput,
                        onValueChange = { editNoteInput = it },
                        label = "Note",
                        modifier = Modifier.testTag("edit_loan_note_input")
                    )
                }
            },
            confirmButton = {
                HisaabButton(
                    text = "Save Changes",
                    enabled = isEditFormValid,
                    onClick = {
                        viewModel.updateLoan(
                            loanId = primaryRawLoan.id,
                            name = editNameInput.trim(),
                            amount = editAmountInput.toDoubleOrNull() ?: 0.0,
                            interestRate = editInterestRateInput.toDoubleOrNull() ?: 0.0,
                            date = editDateInput,
                            note = editNoteInput.trim()
                        ) {
                            showEditDialog = false
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                        }
                    },
                    modifier = Modifier.testTag("save_edit_loan_btn")
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Delete Ledger?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "This action is permanent and will delete this borrower, all associated loan contracts, and complete historical records. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                HisaabButton(
                    text = "Delete Everything",
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.deleteBorrower {
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_btn")
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun TimelineRow(
    item: TimelineItem,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        val dotColor = when (item) {
            is TimelineItem.LoanCreated -> RoyalBlue
            is TimelineItem.MonthlyInterest -> {
                when (item.status) {
                    "PAID" -> EmeraldGreen
                    "PENDING" -> Color(0xFFF59E0B)
                    "PARTIAL" -> Color(0xFF10B981)
                    "OVERDUE" -> MaterialTheme.colorScheme.error
                    else -> RoyalBlue
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 20.dp, start = 8.dp)
        ) {
            HisaabCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                elevation = 1.dp
            ) {
                when (item) {
                    is TimelineItem.LoanCreated -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Loan Disbursed",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalBlue
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.loan.note.ifBlank { "No initial note" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Date: ${item.loan.loanDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = DummyData.formatCurrency(item.loan.amount),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RoyalBlue.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${item.loan.interestRate}% / mo",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBlue
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is TimelineItem.MonthlyInterest -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Month ${item.monthNumber}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = dotColor
                                    )
                                    
                                    val statusText = when (item.status) {
                                        "PAID" -> "Paid"
                                        "PENDING" -> "Pending"
                                        "PARTIAL" -> "Partial"
                                        "OVERDUE" -> "Overdue"
                                        else -> "Upcoming"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(dotColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = dotColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Due Date: ${DummyData.formatDate(item.dueDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.paidDate != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Paid on: ${DummyData.formatDate(item.paidDate)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = EmeraldGreen
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = DummyData.formatCurrency(item.interestAmount),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (item.status == "PAID") EmeraldGreen else dotColor
                                    )
                                )
                                if (item.interestPaid > 0.0) {
                                    Text(
                                        text = "Paid: ${DummyData.formatCurrency(item.interestPaid)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldGreen
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
