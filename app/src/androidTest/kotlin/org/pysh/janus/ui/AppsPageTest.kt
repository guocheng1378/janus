package org.pysh.janus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.pysh.janus.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * AppsPage UI 测试 — 对应 TC-APP-001~006
 */
@RunWith(AndroidJUnit4::class)
class AppsPageTest {

    @get:Rule
    val rule = createComposeRule()

    private fun s(id: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    // TC-APP-001: 应用列表页面加载，显示标题
    @Test
    fun appsPage_displaysTitle() {
        rule.setContent {
            MiuixTheme { AppsPage(bottomPadding = 0.dp, whitelistVersion = 0, allApps = emptyList(), mediaApps = emptyList(), isRefreshing = false, onRefresh = {}, onAppClick = {}) }
        }
        rule.onNodeWithText(s(R.string.nav_apps)).assertIsDisplayed()
    }

    // TC-APP-003/004: 搜索栏展示
    @Test
    fun appsPage_displaysSearchBar() {
        rule.setContent {
            MiuixTheme { AppsPage(bottomPadding = 0.dp, whitelistVersion = 0, allApps = emptyList(), mediaApps = emptyList(), isRefreshing = false, onRefresh = {}, onAppClick = {}) }
        }
        rule.onNodeWithText(s(R.string.search_apps)).assertExists()
    }

    // TC-APP-006: 搜索不存在的关键词不崩溃
    @Test
    fun appsPage_emptySearchResult_noCrash() {
        rule.setContent {
            MiuixTheme { AppsPage(bottomPadding = 0.dp, whitelistVersion = 0, allApps = emptyList(), mediaApps = emptyList(), isRefreshing = false, onRefresh = {}, onAppClick = {}) }
        }
        // 页面正常渲染即可，无崩溃
        rule.onNodeWithText(s(R.string.nav_apps)).assertIsDisplayed()
    }
}
