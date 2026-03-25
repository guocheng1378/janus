package org.pysh.janus.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.res.stringResource
import org.pysh.janus.R
import org.pysh.janus.data.WhitelistManager
import org.pysh.janus.service.ScreenKeepAliveService
import org.pysh.janus.util.DisplayUtils
import org.pysh.janus.util.RootUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

sealed interface Screen : NavKey {
    data object Activation : Screen
    data object Main : Screen
    data object About : Screen
    data class AppFeature(val packageName: String) : Screen
}

@Composable
fun MainScreen(isModuleActive: Boolean) {
    val controller = remember {
        ThemeController(colorSchemeMode = ColorSchemeMode.System)
    }

    MiuixTheme(controller = controller) {
        val context = LocalContext.current
        val whitelistManager = remember { WhitelistManager(context) }

        val initialScreen = remember {
            if (whitelistManager.isActivated()) Screen.Main else Screen.Activation
        }
        val backStack = remember { mutableStateListOf<NavKey>(initialScreen) }

        val scope = rememberCoroutineScope()
        var hasRoot by remember { mutableStateOf<Boolean?>(null) }
        var currentDpi by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(Unit) {
            hasRoot = withContext(Dispatchers.IO) { RootUtils.hasRoot() }
            currentDpi = withContext(Dispatchers.IO) { DisplayUtils.getRearDpi() }
            // 恢复常亮服务
            if (whitelistManager.isKeepAliveEnabled() && !ScreenKeepAliveService.isRunning) {
                ScreenKeepAliveService.start(context, whitelistManager.getKeepAliveInterval())
            }
        }

        // 自动激活：模块和 Root 都就绪时，标记已激活并进入主界面
        LaunchedEffect(isModuleActive, hasRoot) {
            if (isModuleActive && hasRoot == true && !whitelistManager.isActivated()) {
                whitelistManager.setActivated()
                backStack.clear()
                backStack.add(Screen.Main)
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
        ) { key ->
            when (key) {
                Screen.Activation -> NavEntry(key) {
                    ActivationPage(
                        isModuleActive = isModuleActive,
                        hasRoot = hasRoot == true,
                    )
                }
                Screen.Main -> NavEntry(key) {
                    val pagerState = rememberPagerState(pageCount = { 4 })
                    var selectedTab by remember { mutableIntStateOf(0) }

                    PagerBackHandler(pagerState, backStack) {
                        selectedTab = 0
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0; scope.launch { pagerState.animateScrollToPage(0) } },
                                    icon = MiuixIcons.Info,
                                    label = stringResource(R.string.nav_home),
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1; scope.launch { pagerState.animateScrollToPage(1) } },
                                    icon = MiuixIcons.GridView,
                                    label = stringResource(R.string.nav_apps),
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2; scope.launch { pagerState.animateScrollToPage(2) } },
                                    icon = MiuixIcons.Tune,
                                    label = stringResource(R.string.nav_features),
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3; scope.launch { pagerState.animateScrollToPage(3) } },
                                    icon = MiuixIcons.Settings,
                                    label = stringResource(R.string.nav_settings),
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.systemBars,
                    ) { paddingValues ->
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 3,
                            userScrollEnabled = false,
                        ) { page ->
                            when (page) {
                                0 -> HomePage(
                                    bottomPadding = paddingValues.calculateBottomPadding(),
                                    isModuleActive = isModuleActive,
                                    hasRoot = hasRoot,
                                )
                                1 -> AppsPage(
                                    bottomPadding = paddingValues.calculateBottomPadding(),
                                    onAppClick = { app -> backStack.add(Screen.AppFeature(app.packageName)) },
                                )
                                2 -> FeaturesPage(
                                    bottomPadding = paddingValues.calculateBottomPadding(),
                                    currentDpi = currentDpi,
                                    onDpiChanged = { currentDpi = it },
                                )
                                3 -> SettingsPage(
                                    bottomPadding = paddingValues.calculateBottomPadding(),
                                    onAboutClick = { backStack.add(Screen.About) },
                                )
                            }
                        }
                    }
                }
                Screen.About -> NavEntry(key) {
                    AboutPage(onBack = { backStack.removeLastOrNull() })
                }
                is Screen.AppFeature -> NavEntry(key) {
                    AppFeaturePage(
                        packageName = key.packageName,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                else -> NavEntry(key) {}
            }
        }
    }
}

@Composable
private fun PagerBackHandler(
    pagerState: PagerState,
    backStack: List<NavKey>,
    onBackToHome: () -> Unit,
) {
    val isEnabled by remember {
        derivedStateOf {
            backStack.size == 1 && pagerState.currentPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isEnabled,
        onBackCompleted = onBackToHome,
    )
}
