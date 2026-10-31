package com.vknxworks.ridectrl.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.ui.components.LocalBottomClearance
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.TabTitle

/** The Tools tab: the premix calculator and other tools that don't belong to one vehicle. */
@Composable
fun ToolsScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.tab_tools))
        MessageView(
            Icons.Outlined.Calculate,
            stringResource(R.string.tools_empty_title),
            stringResource(R.string.tools_empty_body),
            modifier = Modifier.padding(bottom = LocalBottomClearance.current),
        )
    }
}
