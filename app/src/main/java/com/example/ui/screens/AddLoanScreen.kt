package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HisaabButton
import com.example.ui.components.HisaabCard
import com.example.ui.components.HisaabTextField
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.EmeraldGreen
import com.example.util.DummyData
import com.example.viewmodel.AddLoanViewModel
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    viewModel: AddLoanViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borrowerName by viewModel.borrowerName.collectAsStateWithLifecycle()
    val loanAmount by viewModel.loanAmount.collectAsStateWithLifecycle()
    val interestRate by viewModel.interestRate.collectAsStateWithLifecycle()
    val loanDate by viewModel.loanDate.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Setup DatePickerDialog helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            viewModel.onLoanDateChanged(cal.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form validation check: name not empty, principal > 0, interest rate >= 0, loan date valid
    val amt = loanAmount.toDoubleOrNull() ?: 0.0
    val rate = interestRate.toDoubleOrNull() ?: -1.0
    val isFormValid = borrowerName.isNotBlank() && amt > 0.0 && rate >= 0.0 && loanDate > 0L

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Loan Agreement",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Introductory Card explaining monthly simple interest
            HisaabCard(
                backgroundColor = RoyalBlue.copy(alpha = 0.08f),
                borderColor = RoyalBlue.copy(alpha = 0.2f),
                elevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Simple Interest Information",
                        tint = RoyalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Interest on loans in Hisaab is calculated as Monthly Simple Interest. Ideal for peer-to-peer and shop contracts.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Loan Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Form Fields Card container
            HisaabCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                elevation = 2.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Borrower Name
                    HisaabTextField(
                        value = borrowerName,
                        onValueChange = { viewModel.onBorrowerNameChanged(it) },
                        label = "Borrower Name *",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.testTag("add_loan_name_input")
                    )

                    // Loan Amount
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HisaabTextField(
                            value = loanAmount,
                            onValueChange = { viewModel.onLoanAmountChanged(it) },
                            label = "Principal Amount (₹) *",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.testTag("add_loan_amount_input")
                        )
                        val isAmountInvalid = loanAmount.isNotBlank() && loanAmount.toDoubleOrNull() == null
                        AnimatedVisibility(
                            visible = isAmountInvalid,
                            enter = fadeIn() + expandVertically(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = "Please enter a valid positive number",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Interest Rate
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HisaabTextField(
                            value = interestRate,
                            onValueChange = { viewModel.onInterestRateChanged(it) },
                            label = "Monthly Interest Rate (%) *",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.testTag("add_loan_rate_input")
                        )
                        val isRateInvalid = interestRate.isNotBlank() && interestRate.toDoubleOrNull() == null
                        AnimatedVisibility(
                            visible = isRateInvalid,
                            enter = fadeIn() + expandVertically(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Text(
                                text = "Please enter a valid rate percentage",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                            )
                        }
                    }

                    // Loan Date (Clickable Selector)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { datePickerDialog.show() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Agreement Start Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DummyData.formatDate(loanDate),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date",
                            tint = RoyalBlue
                        )
                    }

                    // Optional Note
                    HisaabTextField(
                        value = note,
                        onValueChange = { viewModel.onNoteChanged(it) },
                        label = "Optional Note / Collateral",
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.height(100.dp)
                    )
                }
            }

            // Live Intel Preview (Stunning simple interest calculation card)
            AnimatedVisibility(
                visible = isFormValid,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.fillMaxWidth()
            ) {
                val amt = loanAmount.toDoubleOrNull() ?: 0.0
                val rate = interestRate.toDoubleOrNull() ?: 0.0
                val monthlyCalculated = amt * (rate / 100.0)
                val annualCalculated = monthlyCalculated * 12.0

                HisaabCard(
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ledger Interest Forecast",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Interest Yield",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DummyData.formatCurrency(monthlyCalculated),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "12-Month Expected Yield",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DummyData.formatCurrency(annualCalculated),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Action with polished dynamic state
            HisaabButton(
                text = if (isSaving) "Saving Contract..." else "Record Agreement",
                enabled = isFormValid && !isSaving,
                onClick = {
                    viewModel.saveLoan {
                        scope.launch {
                            snackbarHostState.showSnackbar("Loan agreement successfully recorded!")
                            kotlinx.coroutines.delay(800)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_loan_button")
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
