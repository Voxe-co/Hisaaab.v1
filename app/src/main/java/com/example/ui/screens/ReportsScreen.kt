package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HisaabCard
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RichBlack
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueLight
import com.example.util.DummyData
import com.example.viewmodel.HomeViewModel
import com.example.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val trendPoints by viewModel.monthlyTrend.collectAsState()
    val distributionPoints by viewModel.principalDistribution.collectAsState()
    val recentActivities by homeViewModel.recentActivities.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column {
                Text(
                    text = "Lending Reports",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Comprehensive yield and risk analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Analytical Statistics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Collection Rate Card
                HisaabCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = EmeraldGreen.copy(alpha = 0.08f),
                    borderColor = EmeraldGreen.copy(alpha = 0.2f),
                    elevation = 0.dp
                ) {
                    Text(
                        text = "Collection Efficiency",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats.collectionRate}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "Up",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Above Target",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cumulative Revenue
                HisaabCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = RoyalBlue.copy(alpha = 0.08f),
                    borderColor = RoyalBlue.copy(alpha = 0.2f),
                    elevation = 0.dp
                ) {
                    Text(
                        text = "Total Interest Earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DummyData.formatCurrency(stats.totalCollectedInterest),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = RoyalBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "Up",
                            tint = RoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+${stats.monthlyGrowthPercent}% MoM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom Canvas Line Graph: Monthly Collection Trend
        item {
            HisaabCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trend_report_card"),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Receipts Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Cumulative monthly simple interest payments (INR)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = "Trend graph",
                        tint = RoyalBlue
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom Line Drawing on Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val spacingX = width / (trendPoints.size - 1)
                        val maxVal = trendPoints.maxOf { pt -> pt.value }
                        val minVal = trendPoints.minOf { pt -> pt.value }
                        val diff = if (maxVal - minVal == 0f) 1f else (maxVal - minVal)

                        val path = Path()
                        val fillPath = Path()

                        // Chart coordinates setup
                        val points = trendPoints.mapIndexed { idx, pt ->
                            val x = idx * spacingX
                            // Invert Y coordinate so larger values are higher on screen
                            val ratio = (pt.value - minVal) / diff
                            val y = height - (ratio * (height - 40.dp.toPx()) + 20.dp.toPx())
                            Offset(x, y)
                        }

                        // Start path drawings
                        path.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, height)
                        fillPath.lineTo(points.first().x, points.first().y)

                        // Draw curved splines
                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val controlX = (p1.x + p2.x) / 2
                            path.cubicTo(
                                controlX, p1.y,
                                controlX, p2.y,
                                p2.x, p2.y
                            )
                            fillPath.cubicTo(
                                controlX, p1.y,
                                controlX, p2.y,
                                p2.x, p2.y
                            )
                        }

                        fillPath.lineTo(points.last().x, height)
                        fillPath.close()

                        // Draw background gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(RoyalBlue.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )

                        // Draw main stroke line
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(RoyalBlue, RoyalBlueLight)),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw glowing joint points
                        points.forEach { pt ->
                            drawCircle(
                                color = RichBlack,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = RoyalBlue,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                // Month labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trendPoints.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Principal distribution overview list
        item {
            HisaabCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("distribution_report_card"),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Principal Exposure",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Share of total outstanding principal by borrower",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Exposure distribution",
                        tint = AmberGold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                distributionPoints.forEach { pt ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${pt.value.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = RoyalBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Bar distribution rendering
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(pt.value / 100f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(RoyalBlue, RoyalBlueLight)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Historical Portfolio Summary Card
        item {
            HisaabCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("historical_summary_report_card"),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Historical Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Overall lending history and ledger stats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Historical summary",
                        tint = RoyalBlue
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoricalRow(label = "Total Active Loans", value = "${stats.activeLoansCount}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    HistoricalRow(label = "Active Borrowers", value = "${stats.activeBorrowersCount}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    HistoricalRow(label = "Total Principal Out", value = DummyData.formatCurrency(stats.totalOutstandingPrincipal))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    HistoricalRow(label = "Total Collected Interest", value = DummyData.formatCurrency(stats.totalCollectedInterest))
                }
            }
        }

        // Recent Activities section
        if (recentActivities.isNotEmpty()) {
            item {
                Text(
                    text = "Activity Log & History",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recentActivities.forEach { activity ->
                        RecentActivityRow(activity = activity)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HistoricalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RecentActivityRow(activity: com.example.database.ActivityLogEntity) {
    val (icon, color, label) = when (activity.type) {
        "LOAN_CREATED" -> Triple(Icons.Default.Add, RoyalBlue, "New Loan Added")
        "INTEREST_RECEIVED" -> Triple(Icons.Default.CurrencyRupee, EmeraldGreen, "Interest Received")
        "PRINCIPAL_RECEIVED" -> Triple(Icons.Default.AccountBalanceWallet, Color(0xFF8B5CF6), "Principal Received")
        "PAYMENT_REVERSED" -> Triple(Icons.Default.Undo, MaterialTheme.colorScheme.error, "Payment Reversed")
        else -> Triple(Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant, "Activity")
    }

    HisaabCard(
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = activity.borrowerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (activity.type == "LOAN_CREATED") {
                        "- ${DummyData.formatCurrency(activity.amount)}"
                    } else if (activity.type == "PAYMENT_REVERSED") {
                        "- ${DummyData.formatCurrency(activity.amount)}"
                    } else {
                        "+ ${DummyData.formatCurrency(activity.amount)}"
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (activity.type == "LOAN_CREATED" || activity.type == "PAYMENT_REVERSED") MaterialTheme.colorScheme.error else EmeraldGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = DummyData.formatDate(activity.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
