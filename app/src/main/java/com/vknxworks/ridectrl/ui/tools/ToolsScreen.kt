package com.vknxworks.ridectrl.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.ui.components.CtrlHeroCard
import com.vknxworks.ridectrl.ui.components.Group
import com.vknxworks.ridectrl.ui.components.GroupRow
import com.vknxworks.ridectrl.ui.components.LocalBottomClearance
import com.vknxworks.ridectrl.ui.components.TabTitle
import com.vknxworks.ridectrl.ui.nav.BackupRoute
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.PartsRoute
import com.vknxworks.ridectrl.ui.nav.PremixRoute
import com.vknxworks.ridectrl.ui.parts.isLow
import com.vknxworks.ridectrl.ui.theme.Gutter

/** The Tools tab: the premix calculator, the shared parts inventory and backup. */
@Composable
fun ToolsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val data by c.logs.data.collectAsStateWithLifecycle()
    val low = data.parts.count { it.isLow() }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp + LocalBottomClearance.current),
    ) {
        TabTitle(stringResource(R.string.tab_tools))
        CtrlHeroCard(
            label = stringResource(R.string.premix_title),
            meta = "25 · 32 · 40 · 50",
            onClick = { nav.navigate(PremixRoute()) },
            modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.tools_premix_big), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tools_premix_sub), style = MaterialTheme.typography.bodyMedium)
        }
        Group {
            GroupRow(
                stringResource(R.string.parts_title),
                if (low > 0) stringResource(R.string.parts_low_count, low) else stringResource(R.string.parts_count, data.parts.size),
                Icons.Outlined.Inventory2,
                showChevron = true,
                onClick = { nav.navigate(PartsRoute) },
            )
            GroupRow(
                stringResource(R.string.backup_title),
                stringResource(R.string.backup_sub),
                Icons.Outlined.Backup,
                showChevron = true,
                onClick = { nav.navigate(BackupRoute) },
            )
        }
    }
}
