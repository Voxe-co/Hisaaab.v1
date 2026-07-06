package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.util.DummyData
import com.example.util.ReportStatsMock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChartPoint(val label: String, val value: Float)

class ReportsViewModel : ViewModel() {

    private val _stats = MutableStateFlow(DummyData.stats)
    val stats: StateFlow<ReportStatsMock> = _stats.asStateFlow()

    // Monthly interest collections trend dummy data
    private val _monthlyTrend = MutableStateFlow(
        listOf(
            ChartPoint("Jan", 12000f),
            ChartPoint("Feb", 15000f),
            ChartPoint("Mar", 14000f),
            ChartPoint("Apr", 18000f),
            ChartPoint("May", 21000f),
            ChartPoint("Jun", 25000f)
        )
    )
    val monthlyTrend: StateFlow<List<ChartPoint>> = _monthlyTrend.asStateFlow()

    // Principal distribution by borrower (percentages)
    private val _principalDistribution = MutableStateFlow(
        listOf(
            ChartPoint("Ramesh K.", 36f),
            ChartPoint("Anita S.", 24f),
            ChartPoint("Sukhwinder S.", 20f),
            ChartPoint("Mohammad A.", 12f),
            ChartPoint("Others", 8f)
        )
    )
    val principalDistribution: StateFlow<List<ChartPoint>> = _principalDistribution.asStateFlow()
}
