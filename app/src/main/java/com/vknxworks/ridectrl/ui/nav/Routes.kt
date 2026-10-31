package com.vknxworks.ridectrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object GarageRoute
@Serializable data object ToolsRoute

// Opened from the profile picture: the You page, and Settings from there
@Serializable data object YouRoute
@Serializable data object SettingsRoute
