package com.vknxworks.ridectrl.ui.nav

import kotlinx.serialization.Serializable

// Tabs
@Serializable data object GarageRoute
@Serializable data object ToolsRoute

// Opened from the profile picture: the You page, and Settings and Backup from there
@Serializable data object YouRoute
@Serializable data object SettingsRoute
@Serializable data object BackupRoute

// Vehicles
@Serializable data class VehicleRoute(val id: String)
@Serializable data class VehicleEditRoute(val id: String? = null)

// Fuel and premix
@Serializable data class FuelRoute(val vehicleId: String)
@Serializable data class FuelEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class PremixRoute(val vehicleId: String? = null)

// Maintenance
@Serializable data class MaintenanceRoute(val vehicleId: String)
@Serializable data class ServiceItemEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class ServiceEditRoute(val vehicleId: String, val id: String? = null, val itemId: String? = null)

// Parts, mods, specs
@Serializable data object PartsRoute
@Serializable data class PartEditRoute(val id: String? = null, val vehicleId: String? = null)
@Serializable data class ModsRoute(val vehicleId: String)
@Serializable data class ModEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class SpecsRoute(val vehicleId: String)

// Temperature, odometer, trips
@Serializable data class TempsRoute(val vehicleId: String)
@Serializable data class TempEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class OdometerRoute(val vehicleId: String)
@Serializable data class OdometerEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class TripEditRoute(val vehicleId: String, val id: String? = null)

// Documents, notes, costs
@Serializable data class DocsRoute(val vehicleId: String)
@Serializable data class DocEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class NotesRoute(val vehicleId: String)
@Serializable data class NoteEditRoute(val vehicleId: String, val id: String? = null)
@Serializable data class CostsRoute(val vehicleId: String)
