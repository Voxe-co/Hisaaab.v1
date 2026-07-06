package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HisaabApplication
import com.example.database.BorrowerEntity
import com.example.database.LoanEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddLoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as HisaabApplication).repository

    private val _borrowerName = MutableStateFlow("")
    val borrowerName: StateFlow<String> = _borrowerName.asStateFlow()

    private val _loanAmount = MutableStateFlow("")
    val loanAmount: StateFlow<String> = _loanAmount.asStateFlow()

    private val _interestRate = MutableStateFlow("")
    val interestRate: StateFlow<String> = _interestRate.asStateFlow()

    private val _loanDate = MutableStateFlow(System.currentTimeMillis())
    val loanDate: StateFlow<Long> = _loanDate.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun onBorrowerNameChanged(value: String) {
        _borrowerName.value = value
    }

    fun onLoanAmountChanged(value: String) {
        // Allow numbers only
        if (value.all { it.isDigit() || it == '.' }) {
            _loanAmount.value = value
        }
    }

    fun onInterestRateChanged(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _interestRate.value = value
        }
    }

    fun onLoanDateChanged(timestamp: Long) {
        _loanDate.value = timestamp
    }

    fun onNoteChanged(value: String) {
        _note.value = value
    }

    fun saveLoan(onSuccess: () -> Unit) {
        val name = borrowerName.value.trim()
        val amountStr = loanAmount.value.trim()
        val rateStr = interestRate.value.trim()
        val dateVal = loanDate.value
        val noteVal = note.value.trim()

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val rate = rateStr.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || amount <= 0.0 || rate <= 0.0) {
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                // Find or create borrower
                val existingBorrower = repository.getBorrowerByName(name)
                val bId = if (existingBorrower == null) {
                    val newB = BorrowerEntity(
                        name = name,
                        phone = "",
                        note = noteVal
                    )
                    repository.insertBorrower(newB)
                } else {
                    existingBorrower.id
                }

                // Save loan
                val loan = LoanEntity(
                    borrowerId = bId,
                    borrowerName = name,
                    amount = amount,
                    interestRate = rate,
                    loanDate = dateVal,
                    note = noteVal
                )
                repository.insertLoan(loan)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
