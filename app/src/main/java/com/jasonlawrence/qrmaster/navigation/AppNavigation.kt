package com.jasonlawrence.qrmaster.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jasonlawrence.qrmaster.ui.generator.GeneratorScreen
import com.jasonlawrence.qrmaster.ui.scanner.ScannerScreen
import com.jasonlawrence.qrmaster.ui.email.EmailScreen
import com.jasonlawrence.qrmaster.viewmodel.QRViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Generator : Screen("generator", "Generate", Icons.Default.Home)
    data object Scanner : Screen("scanner", "Scan", Icons.Default.QrCodeScanner)
    data object Email : Screen("email", "Email", Icons.Default.Email)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: QRViewModel = viewModel()) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Generator, Screen.Scanner, Screen.Email)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Jason's QR Generator")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Generator.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Generator.route) { GeneratorScreen(viewModel) }
            composable(Screen.Scanner.route) { ScannerScreen() }
            composable(Screen.Email.route) { EmailScreen(viewModel) }
        }
    }
}
