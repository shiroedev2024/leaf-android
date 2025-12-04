package com.github.shiroedev2024.leaf.android.screen

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.Utils.isValidUUID
import com.github.shiroedev2024.leaf.android.viewmodel.LeafViewModel
import com.github.shiroedev2024.leaf.android.viewmodel.UpdateViewModel
import java.util.Locale

val screens = listOf(Screen.Dashboard, Screen.Profile, Screen.Settings)

sealed class Screen(val route: String, val icon: Int, val title: Int) {
    data object Dashboard :
        Screen("dashboard", R.drawable.baseline_dashboard_24, R.string.dashboard)

    data object Profile : Screen("profile", R.drawable.baseline_subscriptions_24, R.string.profile)

    data object Settings : Screen("settings", R.drawable.baseline_settings_24, R.string.settings)
}

@Composable
fun NavController.currentRoute(): String? {
    val navBackStackEntry by currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        graph.startDestinationRoute?.let { popUpTo(it) { saveState = true } }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentRoute()

    NavigationBar {
        screens.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = screen.icon),
                        contentDescription = stringResource(screen.title),
                    )
                },
                label = { Text(text = stringResource(screen.title)) },
                selected = currentRoute == screen.route,
                onClick = { navController.navigateSingleTop(screen.route) },
                alwaysShowLabel = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    leafViewModel: LeafViewModel,
    updateViewModel: UpdateViewModel,
    onUpdateLocale: (locale: Locale) -> Unit,
    onStartLeaf: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    var clipboardImportDialogVisible by remember { mutableStateOf(false) }
    var clipboardProfileText by remember { mutableStateOf<String?>(null) }

    fun importFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val clipboardText = clipData.getItemAt(0).text?.toString().orEmpty()
            if (isValidUUID(clipboardText)) {
                clipboardProfileText = clipboardText
                clipboardImportDialogVisible = true
            } else {
                Toast.makeText(context, R.string.clipboard_no_valid_client_id, Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            Toast.makeText(context, R.string.clipboard_empty, Toast.LENGTH_SHORT).show()
        }
    }

    fun onConfirmImport(profileText: String) {
        val encoded = Base64.encodeToString(profileText.toByteArray(), Base64.NO_WRAP)
        val uri = "leafvpn://install?profile=$encoded".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            when (currentRoute) {
                Screen.Dashboard.route -> {
                    TopAppBar(title = { Text(text = stringResource(R.string.dashboard)) })
                }
                Screen.Profile.route,
                Screen.Settings.route -> {
                    TopAppBar(
                        title = {
                            Text(
                                text =
                                    stringResource(
                                        when (currentRoute) {
                                            Screen.Profile.route -> R.string.profile
                                            Screen.Settings.route -> R.string.settings
                                            else -> R.string.unknown
                                        }
                                    )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    painter =
                                        painterResource(id = R.drawable.baseline_arrow_back_24),
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        },
                    )
                }
            }
        },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            if (currentRoute == Screen.Profile.route) {
                FloatingActionButton(onClick = { importFromClipboard() }) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(id = R.drawable.baseline_content_paste_24),
                        contentDescription = stringResource(R.string.import_profile_clipboard),
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardContent(
                    leafViewModel,
                    updateViewModel,
                    onStartLeaf = onStartLeaf,
                    onNavigateToProfile = { navController.navigateSingleTop(Screen.Profile.route) },
                )
            }
            composable(Screen.Profile.route) { ProfileContent(leafViewModel) }
            composable(Screen.Settings.route) {
                SettingsContent(leafViewModel, updateViewModel) { locale -> onUpdateLocale(locale) }
            }
        }
    }
    if (clipboardImportDialogVisible && clipboardProfileText != null) {
        ClipboardImportDialog(
            profileText = clipboardProfileText,
            onDismiss = {
                clipboardImportDialogVisible = false
                clipboardProfileText = null
            },
            onConfirm = { profileText ->
                onConfirmImport(profileText)
                clipboardImportDialogVisible = false
                clipboardProfileText = null
            },
        )
    }
}
