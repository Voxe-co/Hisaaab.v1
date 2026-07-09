package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.RoyalBlue
import com.example.viewmodel.*

// Core routes
object Routes {
    const val SPLASH = "splash"
    const val MAIN_TABS = "main_tabs"
    const val ADD_LOAN = "add_loan"
    const val BORROWER_DETAILS = "borrower_details"
}

// Inner Tab destinations
enum class TabDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("tab_dashboard", "Dashboard", Icons.Default.Home),
    REPORTS("tab_reports", "Reports", Icons.Default.BarChart),
    SETTINGS("tab_settings", "Settings", Icons.Default.Settings)
}

@Composable
fun HisaabNavigation(
    onToggleTheme: () -> Unit,
    borrowerIdToOpen: Long?,
    onClearOpenRequest: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    LaunchedEffect(borrowerIdToOpen) {
        if (borrowerIdToOpen != null) {
            // Wait for splash screen to clear or navigate to main tabs if not currently there
            if (navController.currentDestination?.route == Routes.SPLASH) {
                navController.navigate(Routes.MAIN_TABS) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
            navController.navigate("${Routes.BORROWER_DETAILS}/$borrowerIdToOpen") {
                launchSingleTop = true
            }
            onClearOpenRequest()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        // 1. Splash Screen
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.MAIN_TABS) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // 2. Main Persistent Tabs Container
        composable(Routes.MAIN_TABS) {
            MainTabsContainer(
                onNavigateToAddLoan = { navController.navigate(Routes.ADD_LOAN) },
                onNavigateToBorrowerDetails = { borrowerId ->
                    navController.navigate("${Routes.BORROWER_DETAILS}/$borrowerId")
                },
                onToggleTheme = onToggleTheme
            )
        }

        // 3. Add Loan Screen
        composable(
            route = Routes.ADD_LOAN,
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
            }
        ) {
            val addLoanViewModel: AddLoanViewModel = viewModel()
            AddLoanScreen(
                viewModel = addLoanViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Borrower Details Screen
        composable(
            route = "${Routes.BORROWER_DETAILS}/{borrowerId}",
            arguments = listOf(navArgument("borrowerId") { type = NavType.LongType }),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val borrowerId = backStackEntry.arguments?.getLong("borrowerId") ?: 1L
            val borrowerDetailsViewModel: BorrowerDetailsViewModel = viewModel()
            BorrowerDetailsScreen(
                borrowerId = borrowerId,
                viewModel = borrowerDetailsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsContainer(
    onNavigateToAddLoan: () -> Unit,
    onNavigateToBorrowerDetails: (Long) -> Unit,
    onToggleTheme: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    reportsViewModel: ReportsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(TabDestination.DASHBOARD) }
    val view = LocalView.current

    Scaffold(
        bottomBar = {
            // High-fidelity bottom navigation bar with rounded corners and subtle background frosting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .windowInsetsPadding(WindowInsets.navigationBars) // Respect Android gesture bar safe-area
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("bottom_nav_bar"),
                ) {
                    TabDestination.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (selectedTab != tab) {
                                    try {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                                    } catch (e: Exception) {}
                                    selectedTab = tab
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = RoyalBlue,
                                indicatorColor = RoyalBlue,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing, // Perfect Edge-to-edge support
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab contents animations using Crossfade for super smooth, fast state transitions
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(300),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    TabDestination.DASHBOARD -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToAddLoan = onNavigateToAddLoan,
                            onNavigateToBorrowerDetails = onNavigateToBorrowerDetails,
                            onNavigateToReports = { selectedTab = TabDestination.REPORTS },
                            onNavigateToSettings = { selectedTab = TabDestination.SETTINGS }
                        )
                    }
                    TabDestination.REPORTS -> {
                        ReportsScreen(
                            viewModel = reportsViewModel,
                            homeViewModel = homeViewModel
                        )
                    }
                    TabDestination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onToggleTheme = onToggleTheme
                        )
                    }
                }
            }
        }
    }
}
