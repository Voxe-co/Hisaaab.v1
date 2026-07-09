package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HisaabCard
import com.example.ui.components.HisaabTextField
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueLight
import com.example.util.DummyData
import com.example.util.LoanMock
import com.example.viewmodel.HomeViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.viewmodel.AdvancedFilterOption
import com.example.viewmodel.parseNaturalLanguageInput
import java.io.File
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddLoan: () -> Unit,
    onNavigateToBorrowerDetails: (Long) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val todayCollection by viewModel.todayCollection.collectAsStateWithLifecycle()
    val upcomingCollection by viewModel.upcomingCollection.collectAsStateWithLifecycle()
    val overdueCollection by viewModel.overdueCollection.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()

    var showEmptyStateDemo by remember { mutableStateOf(false) }
    var showReceivePaymentDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showQuickEntryDialog by remember { mutableStateOf(false) }
    var showLongPressSheet by remember { mutableStateOf(false) }
    var selectedLoanForActions by remember { mutableStateOf<LoanMock?>(null) }

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCollectionsBottomSheet by remember { mutableStateOf(false) }
    var collectionsSelectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(showEmptyStateDemo) {
        viewModel.toggleEmptyState(showEmptyStateDemo)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.lastDeletedLoanNameFlow.collect { borrowerName ->
            val result = snackbarHostState.showSnackbar(
                message = "Loan for $borrowerName deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteLoan()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSearchExpanded) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Search borrowers, notes...", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = RoyalBlue
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("appbar_search_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.onSearchQueryChanged("")
                            isSearchExpanded = false
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Collapse Search"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF3B82F6),
                                                Color(0xFF1D4ED8)
                                            )
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                    contentDescription = "Hisaab Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                )
                            }
                            Text(
                                text = "Hisaab",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchExpanded = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { /* Optional notification action */ },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddLoan,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier
                    .padding(bottom = 38.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = RoyalBlue.copy(alpha = 0.5f),
                        spotColor = RoyalBlue
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RoyalBlue, RoyalBlueLight)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("add_loan_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Loan",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Spacer for clean layout breathing room
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Summary Metric Card (Royal Blue Hero)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(RoyalBlue, RoyalBlueLight)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Outstanding",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = DummyData.formatCurrency(stats.totalOutstandingPrincipal),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    letterSpacing = (-1).sp
                                ),
                                color = Color.White
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Trending Up",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "+${DummyData.formatCurrency(stats.totalMonthlyInterestExpected)} expected this month",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Filters and Sort Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horizontally scrollable Filter Chips
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        val filterPairs = listOf(
                            AdvancedFilterOption.ALL to "All",
                            AdvancedFilterOption.CURRENT to "Current",
                            AdvancedFilterOption.DUE_TODAY to "Pending",
                            AdvancedFilterOption.OVERDUE to "Overdue",
                            AdvancedFilterOption.COMPLETED to "Completed"
                        )
                        items(filterPairs) { (option, label) ->
                            val isSelected = activeFilter == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onFilterChanged(option) },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = RoyalBlue,
                                    selectedLeadingIconColor = RoyalBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_chip_${label.lowercase()}")
                            )
                        }
                    }

                    // Sort IconButton and DropdownMenu
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("sort_loans_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort Loans",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            val sortPairs = listOf(
                                AdvancedFilterOption.NEWEST to "Newest",
                                AdvancedFilterOption.OLDEST to "Oldest",
                                AdvancedFilterOption.HIGHEST_INTEREST to "Highest Interest",
                                AdvancedFilterOption.LOWEST_INTEREST to "Lowest Interest",
                                AdvancedFilterOption.HIGHEST_PRINCIPAL to "Highest Principal",
                                AdvancedFilterOption.LOWEST_PRINCIPAL to "Lowest Principal"
                            )
                            sortPairs.forEach { (option, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.onFilterChanged(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (option) {
                                                AdvancedFilterOption.NEWEST -> Icons.Default.ArrowDownward
                                                AdvancedFilterOption.OLDEST -> Icons.Default.ArrowUpward
                                                AdvancedFilterOption.HIGHEST_INTEREST, AdvancedFilterOption.HIGHEST_PRINCIPAL -> Icons.Default.TrendingUp
                                                else -> Icons.Default.TrendingDown
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (activeFilter == option) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = if (activeFilter == option) RoyalBlue else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("sort_option_${label.replace(" ", "_").lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Loans Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Active Loan Contracts",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage and track outstanding ledgers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoyalBlue.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${loans.size} ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue
                            )
                        )
                    }
                }
            }

            // List or Empty State
            if (loans.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PremiumEmptyStateIllustration()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No loans yet",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No active loans match your filter criteria. Try searching another name."
                            } else {
                                "Create a loan contract to manage monthly interest tracking and ledgers."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToAddLoan,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        clip = false,
                                        ambientColor = RoyalBlue.copy(alpha = 0.4f),
                                        spotColor = RoyalBlue
                                    )
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(RoyalBlue, RoyalBlueLight)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Create First Loan",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(loans, key = { it.id }) { loan ->
                    SwipeableActiveLoanRow(
                        loan = loan,
                        onClick = { onNavigateToBorrowerDetails(loan.borrowerId) },
                        onSwipeRight = {
                            viewModel.receiveInterestPayment(
                                loanId = loan.id,
                                amount = loan.monthlyInterestAmount,
                                note = "Monthly interest swiped received from Home Screen"
                            )
                        },
                        onSwipeLeft = {
                            viewModel.receivePrincipalPayment(
                                loanId = loan.id,
                                amount = loan.amount / 10.0, // Quick principal payment of 10%
                                note = "Part principal payment swiped received from Home Screen"
                            )
                        },
                        onLongPress = {
                            selectedLoanForActions = loan
                            showLongPressSheet = true
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Avoid content cut off by custom nav bar
            }
        }
    }

    if (showReceivePaymentDialog) {
        ReceivePaymentDialog(
            loans = loans,
            onDismiss = { showReceivePaymentDialog = false },
            onReceive = { loanId, amount, isInterest, note ->
                if (isInterest) {
                    viewModel.receiveInterestPayment(loanId, amount, note)
                } else {
                    viewModel.receivePrincipalPayment(loanId, amount, note)
                }
                showReceivePaymentDialog = false
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            loans = loans,
            onDismiss = { showExportDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showQuickEntryDialog) {
        QuickEntryDialog(
            onDismiss = { showQuickEntryDialog = false },
            onConfirm = { input ->
                viewModel.createParsedLoanFromNaturalLanguage(input)
                showQuickEntryDialog = false
            }
        )
    }

    if (showLongPressSheet && selectedLoanForActions != null) {
        LongPressActionsSheet(
            loan = selectedLoanForActions!!,
            viewModel = viewModel,
            onDismiss = { showLongPressSheet = false },
            onNavigateToTimeline = {
                showLongPressSheet = false
                onNavigateToBorrowerDetails(selectedLoanForActions!!.borrowerId)
            }
        )
    }

    if (showCollectionsBottomSheet) {
        CollectionsBottomSheet(
            todayCollection = todayCollection,
            upcomingCollection = upcomingCollection,
            overdueCollection = overdueCollection,
            selectedTab = collectionsSelectedTab,
            onTabSelected = { collectionsSelectedTab = it },
            onNavigateToBorrowerDetails = onNavigateToBorrowerDetails,
            onReceiveQuickPayment = { loanId, amount ->
                viewModel.receiveQuickPayment(loanId, amount)
            },
            onDismiss = { showCollectionsBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsBottomSheet(
    todayCollection: List<com.example.viewmodel.TodayCollectionItem>,
    upcomingCollection: List<com.example.viewmodel.UpcomingCollectionItem>,
    overdueCollection: List<com.example.viewmodel.OverdueCollectionItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToBorrowerDetails: (Long) -> Unit,
    onReceiveQuickPayment: (Long, Double) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Collections Tracking",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Custom Capsule Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "Overdue (${overdueCollection.size})" to MaterialTheme.colorScheme.error,
                    "Today (${todayCollection.size})" to EmeraldGreen,
                    "Upcoming (${upcomingCollection.size})" to RoyalBlue
                )

                tabs.forEachIndexed { index, (label, color) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.background
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onTabSelected(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                            ),
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Overdue
                            if (overdueCollection.isEmpty()) {
                                item {
                                    EmptyStateView(
                                        message = "No overdue payments. Excellent credit health!",
                                        color = EmeraldGreen
                                    )
                                }
                            } else {
                                items(overdueCollection) { item ->
                                    OverdueItemRow(
                                        item = item,
                                        onViewDetails = {
                                            onDismiss()
                                            onNavigateToBorrowerDetails(item.borrowerId)
                                        }
                                    )
                                }
                            }
                        }
                        1 -> { // Today
                            if (todayCollection.isEmpty()) {
                                item {
                                    EmptyStateView(
                                        message = "No collection items due today.",
                                        color = RoyalBlue
                                    )
                                }
                            } else {
                                items(todayCollection) { item ->
                                    TodayItemRow(
                                        item = item,
                                        onReceivePayment = {
                                            onReceiveQuickPayment(item.loanId, item.interestAmount)
                                        }
                                    )
                                }
                            }
                        }
                        2 -> { // Upcoming
                            if (upcomingCollection.isEmpty()) {
                                item {
                                    EmptyStateView(
                                        message = "No upcoming payments in the next 7 days.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(upcomingCollection) { item ->
                                    UpcomingItemRow(
                                        item = item
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

@Composable
fun EmptyStateView(message: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun OverdueItemRow(
    item: com.example.viewmodel.OverdueCollectionItem,
    onViewDetails: () -> Unit
) {
    HisaabCard(
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.borrowerName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.daysOverdue} DAYS OVERDUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Pending Interest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DummyData.formatCurrency(item.pendingInterest),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "Current Cycle Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DummyData.formatCurrency(item.currentDue),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            IconButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = RoyalBlue
                )
            }
        }
    }
}

@Composable
fun TodayItemRow(
    item: com.example.viewmodel.TodayCollectionItem,
    onReceivePayment: () -> Unit
) {
    HisaabCard(
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.borrowerName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DUE TODAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        )
                    }
                    Text(
                        text = DummyData.formatDate(item.dueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DummyData.formatCurrency(item.interestAmount),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen
                    )
                )
            }

            Button(
                onClick = onReceivePayment,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Quick Receive",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Receive",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun UpcomingItemRow(
    item: com.example.viewmodel.UpcomingCollectionItem
) {
    HisaabCard(
        elevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.borrowerName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Due in ${item.daysUntilDue} days (${DummyData.formatDate(item.dueDate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = DummyData.formatCurrency(item.interestAmount),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalBlue
                )
            )
        }
    }
}

@Composable
fun ActiveLoanRow(
    loan: LoanMock,
    onClick: () -> Unit
) {
    val view = LocalView.current

    HisaabCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loan_item_${loan.id}"),
        elevation = 2.dp,
        onClick = {
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            } catch (e: Exception) {}
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Borrower Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RoyalBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val isBank = loan.borrowerName.contains("bank", ignoreCase = true) || loan.note.contains("bank", ignoreCase = true)
                Icon(
                    imageVector = if (isBank) Icons.Default.AccountBalance else Icons.Default.Person,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Center Content: Name, Principal, Due Date, Current Month
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = loan.borrowerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (loan.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Principal Amount & Month info (Minimal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = DummyData.formatCurrency(loan.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBlue
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = loan.currentMonth,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Due Date
                Text(
                    text = "Due: ${loan.loanDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Right Content: Interest due, Status Chip
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Interest due
                Text(
                    text = DummyData.formatCurrency(loan.currentInterestDue),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldGreen
                    )
                )

                // Status chip (Paid / Pending / Overdue)
                val (statusText, statusBg, statusColor) = when {
                    loan.pillStatus == "OVERDUE" -> Triple("Overdue", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
                    loan.pendingInterest > 0.0 || loan.pillStatus == "DUE" -> Triple("Pending", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B))
                    else -> Triple("Paid", EmeraldGreen.copy(alpha = 0.15f), EmeraldGreen)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            // Small Arrow Icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SwipeableActiveLoanRow(
    loan: LoanMock,
    onClick: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    val swipeThreshold = 220f
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        } catch (e: Exception) {}
                        onLongPress()
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > swipeThreshold) {
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                            onSwipeRight()
                        } else if (offsetX < -swipeThreshold) {
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            } catch (e: Exception) {}
                            onSwipeLeft()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-350f, 350f)
                    }
                )
            }
    ) {
        if (offsetX != 0f) {
            val isSwipingRight = offsetX > 0
            val bgColor = if (isSwipingRight) EmeraldGreen.copy(alpha = 0.15f) else RoyalBlue.copy(alpha = 0.15f)
            val align = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd
            val icon = if (isSwipingRight) Icons.Default.CurrencyRupee else Icons.Default.AccountBalanceWallet
            val text = if (isSwipingRight) "Receive Interest" else "Receive Principal"
            val tint = if (isSwipingRight) EmeraldGreen else RoyalBlue

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = align
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSwipingRight) {
                        Icon(imageVector = icon, contentDescription = text, tint = tint)
                        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = tint)
                    } else {
                        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = tint)
                        Icon(imageVector = icon, contentDescription = text, tint = tint)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        ) {
            ActiveLoanRow(
                loan = loan,
                onClick = onClick
            )
        }
    }
}

@Composable
fun GlassActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = RoyalBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
fun ReceivePaymentDialog(
    loans: List<LoanMock>,
    onDismiss: () -> Unit,
    onReceive: (Long, Double, Boolean, String) -> Unit
) {
    if (loans.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Active Loans", fontWeight = FontWeight.Bold) },
            text = { Text("Please create a loan before receiving payments.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK", color = RoyalBlue) }
            }
        )
        return
    }

    var selectedIndex by remember { mutableStateOf(0) }
    val selectedLoan = loans[selectedIndex]
    var amountStr by remember { mutableStateOf(selectedLoan.monthlyInterestAmount.toString()) }
    var isInterest by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Borrower: ${selectedLoan.borrowerName}")
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        loans.forEachIndexed { idx, l ->
                            DropdownMenuItem(
                                text = { Text(l.borrowerName) },
                                onClick = {
                                    selectedIndex = idx
                                    amountStr = if (isInterest) l.monthlyInterestAmount.toString() else l.amount.toString()
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isInterest = true
                            amountStr = selectedLoan.monthlyInterestAmount.toString()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInterest) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isInterest) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Interest", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            isInterest = false
                            amountStr = selectedLoan.amount.toString()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isInterest) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isInterest) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Principal", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onReceive(selectedLoan.id, amt, isInterest, note)
                }
            ) {
                Text("Receive", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportDialog(
    loans: List<LoanMock>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("CSV") }

    if (isExporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Exporting...", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                    Text("Generating clean premium report...")
                }
            },
            confirmButton = {}
        )

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            try {
                val cacheFile = File(context.cacheDir, "Hisaab_Report_${System.currentTimeMillis()}.csv")
                cacheFile.bufferedWriter().use { writer ->
                    writer.write("Borrower,Principal,Interest Rate,Monthly Interest,Outstanding,Status\n")
                    loans.forEach { l ->
                        writer.write("${l.borrowerName},${l.amount},${l.interestRate},${l.monthlyInterestAmount},${l.overallAmountDue},${l.pillStatus}\n")
                    }
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.example.fileprovider",
                    cacheFile
                )
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Hisaab Clean Report"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isExporting = false
            onDismiss()
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Premium Exports", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Export premium financial statements with payment histories, interest records, and remaining balances.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PDF", "CSV", "Excel").forEach { format ->
                        val isSel = exportFormat == format
                        Button(
                            onClick = { exportFormat = format },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(format, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { isExporting = true }) {
                Text("Export & Share", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var backupStatus by remember { mutableStateOf("Idle") }
    var autoBackupEnabled by remember { mutableStateOf(true) }

    if (backupStatus == "Backing up...") {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Running Backup", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = RoyalBlue)
                    Text("Copying core SQLite schemas...")
                }
            },
            confirmButton = {}
        )

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            try {
                val dbFile = context.getDatabasePath("hisaab_database")
                if (dbFile.exists()) {
                    val backupFile = File(context.cacheDir, "hisaab_backup_${System.currentTimeMillis()}.db")
                    dbFile.copyTo(backupFile, overwrite = true)
                    
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.example.fileprovider",
                        backupFile
                    )
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Save Hisaab Local Backup"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            backupStatus = "Idle"
            onDismiss()
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup & Cloud Recovery", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Protect your lending business data with real-time encrypted manual backups and prepare cloud architecture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Automatic Daily Backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Quietly backup locally on app exit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { autoBackupEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RoyalBlue, checkedTrackColor = RoyalBlue.copy(alpha = 0.5f))
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Button(
                    onClick = {
                        // prepare
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CloudQueue, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Prepare Firebase Cloud Sync", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { backupStatus = "Backing up..." }) {
                Text("Backup Now", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun QuickEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val parsedResult = remember(input) { parseNaturalLanguageInput(input) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Natural Language Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Type in natural language to quickly create a contract.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Examples:\n• Rajesh ko 50000 diya 3 percent pe\n• Sanya 20000 2%",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = RoyalBlue
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Natural Language Input") },
                    placeholder = { Text("e.g. Ramesh ko 10000 diya 2 percent pe") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (input.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RoyalBlue.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        if (parsedResult != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("✓ PARSED LIVE CONTRACT PREVIEW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen))
                                Text("Borrower: ${parsedResult.borrowerName}", fontWeight = FontWeight.Bold)
                                Text("Principal Amount: ${DummyData.formatCurrency(parsedResult.amount)}")
                                Text("Interest Rate: ${parsedResult.interestRate}% Monthly")
                            }
                        } else {
                            Text("❌ Typing contract structure...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(input) },
                enabled = parsedResult != null
            ) {
                Text("Create Contract", color = RoyalBlue, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LongPressActionsSheet(
    loan: LoanMock,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onNavigateToTimeline: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current
    
    var showEditDialog by remember { mutableStateOf(false) }
    var borrowerTag by remember { mutableStateOf(loan.tag ?: "") }
    var borrowerNote by remember { mutableStateOf(loan.note) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Tag & Notes", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = borrowerTag,
                        onValueChange = { borrowerTag = it },
                        label = { Text("Tag (e.g. Friend, Family, Business)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = borrowerNote,
                        onValueChange = { borrowerNote = it },
                        label = { Text("Borrower Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateBorrowerTagAndNote(loan.borrowerId, borrowerTag, borrowerNote)
                        showEditDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Save", color = RoyalBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = loan.borrowerName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Outstanding: ${DummyData.formatCurrency(loan.overallAmountDue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                        } catch (e: Exception) {}
                        viewModel.toggleFavorite(loan.borrowerId)
                    }
                ) {
                    Icon(
                        imageVector = if (loan.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (loan.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))

            SheetActionRow(
                icon = Icons.Default.Edit,
                label = "Edit Tag & Notes",
                onClick = { showEditDialog = true }
            )

            SheetActionRow(
                icon = Icons.Default.Timeline,
                label = "View Loan Timeline",
                onClick = onNavigateToTimeline
            )

            SheetActionRow(
                icon = Icons.Default.Send,
                label = "WhatsApp Reminder",
                onClick = {
                    try {
                        val msg = "Hisaab Reminder: Hello ${loan.borrowerName}, your interest payment of ${DummyData.formatCurrency(loan.monthlyInterestAmount)} for your loan is currently pending. Please clear it at your earliest. Thanks!"
                        val encodedMsg = Uri.encode(msg)
                        val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg"))
                        context.startActivity(waIntent)
                    } catch (e: Exception) {}
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Default.Call,
                label = "Call Borrower",
                onClick = {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${loan.borrowerId}"))
                        context.startActivity(dialIntent)
                    } catch (e: Exception) {}
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Default.ContentCopy,
                label = "Copy Loan Details",
                onClick = {
                    val details = "Hisaab Loan Details:\nBorrower: ${loan.borrowerName}\nPrincipal: ${DummyData.formatCurrency(loan.amount)}\nInterest Rate: ${loan.interestRate}% Monthly\nOutstanding: ${DummyData.formatCurrency(loan.overallAmountDue)}"
                    clipboardManager.setText(AnnotatedString(details))
                    onDismiss()
                }
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

            SheetActionRow(
                icon = Icons.Default.Delete,
                label = "Delete Borrower & Contract",
                color = MaterialTheme.colorScheme.error,
                onClick = {
                    viewModel.deleteBorrower(loan.borrowerId)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = color)
        }
    }
}

@Composable
fun PremiumStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier: Modifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .then(clickableModifier)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PremiumEmptyStateIllustration() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing background accent
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RoyalBlue.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        // Draw elegant layered finance documents/cards
        Box(
            modifier = Modifier
                .size(90.dp, 60.dp)
                .graphicsLayer {
                    rotationZ = -12f
                    translationX = -20f
                    translationY = 10f
                }
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = Color(0xFF1C1C1E).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(90.dp, 60.dp)
                .graphicsLayer {
                    rotationZ = 8f
                    translationX = 20f
                    translationY = -5f
                }
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = Color(0xFF1C1C1E).copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        // Main focused card
        Box(
            modifier = Modifier
                .size(100.dp, 70.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false,
                    ambientColor = RoyalBlue.copy(alpha = 0.3f),
                    spotColor = RoyalBlue.copy(alpha = 0.5f)
                )
                .border(
                    width = 1.dp,
                    color = RoyalBlue.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Glowy target center icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Mock micro data lines
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(24.dp, 4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.size(12.dp, 4.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
