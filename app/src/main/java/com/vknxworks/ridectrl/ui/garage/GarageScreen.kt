package com.vknxworks.ridectrl.ui.garage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.ui.components.LocalBottomClearance
import com.vknxworks.ridectrl.ui.components.MessageView
import com.vknxworks.ridectrl.ui.components.TabTitle

/** The Garage tab: every vehicle, each opening its own dashboard. */
@Composable
fun GarageScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.tab_garage))
        MessageView(
            Icons.Outlined.TwoWheeler,
            stringResource(R.string.garage_empty_title),
            stringResource(R.string.garage_empty_body),
            modifier = Modifier.padding(bottom = LocalBottomClearance.current),
        )
    }
}
