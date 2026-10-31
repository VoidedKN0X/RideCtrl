package com.vknxworks.ridectrl.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.EngineType
import com.vknxworks.ridectrl.data.OdometerReading
import com.vknxworks.ridectrl.data.SCOOTER_TEMPLATE
import com.vknxworks.ridectrl.data.ServiceItem
import com.vknxworks.ridectrl.data.Vehicle
import com.vknxworks.ridectrl.data.currentKm
import com.vknxworks.ridectrl.data.isTwoStroke
import com.vknxworks.ridectrl.data.newId
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.data.today
import com.vknxworks.ridectrl.ui.components.ChoiceInput
import com.vknxworks.ridectrl.ui.components.DateInput
import com.vknxworks.ridectrl.ui.components.FieldHeading
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.FormScreen
import com.vknxworks.ridectrl.ui.components.NumberInput
import com.vknxworks.ridectrl.ui.components.PhotoStrip
import com.vknxworks.ridectrl.ui.components.SwitchInput
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.nav.LocalNavigator
import com.vknxworks.ridectrl.ui.nav.VehicleRoute

@Composable
fun engineLabel(e: EngineType): String = stringResource(
    when (e) {
        EngineType.TWO_STROKE_PREMIX -> R.string.engine_2t_premix
        EngineType.TWO_STROKE_INJECTION -> R.string.engine_2t_injection
        EngineType.FOUR_STROKE -> R.string.engine_4t
        EngineType.ELECTRIC -> R.string.engine_electric
    },
)

/** Add or edit a vehicle. A new 2-stroke vehicle can start with the scooter service list (intervals blank). */
@Composable
fun VehicleEditScreen(id: String?) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val fmt = rememberFmt()
    val data = c.logs.data.value
    val old = remember(id) { data.vehicles.firstOrNull { it.id == id } }
    val isNew = old == null

    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var make by rememberSaveable { mutableStateOf(old?.make ?: "") }
    var model by rememberSaveable { mutableStateOf(old?.model ?: "") }
    var year by rememberSaveable { mutableStateOf(old?.year?.toString() ?: "") }
    var engine by rememberSaveable { mutableStateOf(old?.engine ?: EngineType.TWO_STROKE_PREMIX) }
    var plate by rememberSaveable { mutableStateOf(old?.plate ?: "") }
    var vin by rememberSaveable { mutableStateOf(old?.vin ?: "") }
    var purchaseDate by rememberSaveable { mutableStateOf(old?.purchaseDate) }
    var price by rememberSaveable { mutableStateOf(fmt.moneyInput(old?.purchasePrice)) }
    var odo by rememberSaveable { mutableStateOf(fmt.distanceInput(old?.let { data.currentKm(it.id) })) }
    var photo by rememberSaveable { mutableStateOf(old?.photo) }
    var ratio by rememberSaveable { mutableStateOf(old?.premixRatio?.toString() ?: "") }
    var oil by rememberSaveable { mutableStateOf(old?.oilBrand ?: "") }
    var notes by rememberSaveable { mutableStateOf(old?.notes ?: "") }
    var template by rememberSaveable { mutableStateOf(true) }

    val yearOk = year.isBlank() || parseInt(year)?.let { it in 1885..2100 } == true
    val odoOk = odo.isBlank() || (parseNumber(odo)?.let { it >= 0 } == true)
    val canSave = name.isNotBlank() && yearOk && odoOk

    FormScreen(
        title = stringResource(if (isNew) R.string.vehicle_add else R.string.vehicle_edit),
        canSave = canSave,
        onSave = {
            val vid = old?.id ?: newId()
            val vehicle = (old ?: Vehicle(id = vid, name = "", created = System.currentTimeMillis())).copy(
                name = name.trim(),
                make = make.trim(),
                model = model.trim(),
                year = parseInt(year),
                engine = engine,
                plate = plate.trim(),
                vin = vin.trim(),
                purchaseDate = purchaseDate,
                purchasePrice = parseNumber(price),
                photo = photo,
                premixRatio = parseInt(ratio)?.takeIf { it in 10..200 },
                oilBrand = oil.trim(),
                notes = notes.trim(),
            )
            val km = parseNumber(odo)?.let { fmt.userToKm(it) }
            c.logs.update { d ->
                var n = if (isNew) d.copy(vehicles = d.vehicles + vehicle) else d.copy(vehicles = d.vehicles.map { if (it.id == vid) vehicle else it })
                // A new odometer value is logged as a reading, so the history stays complete.
                if (km != null && km != d.currentKm(vid)) {
                    n = n.copy(odometer = n.odometer + OdometerReading(newId(), vid, today(), km, context.getString(R.string.odo_note_from_vehicle)))
                }
                if (isNew && template && engine.isTwoStroke) {
                    n = n.copy(
                        serviceItems = n.serviceItems + SCOOTER_TEMPLATE.map { (nameRes, noteRes) ->
                            ServiceItem(newId(), vid, context.getString(nameRes), notes = noteRes?.let { context.getString(it) } ?: "")
                        },
                    )
                }
                n
            }
            if (isNew) nav.replace(VehicleRoute(vid)) else nav.back()
        },
        onDelete = if (isNew) null else ({
            val vid = old!!.id
            c.logs.update { it.withoutVehicle(vid) }
            nav.toGarage()
        }),
        deleteTitle = stringResource(R.string.vehicle_delete_title),
        deleteBody = stringResource(R.string.vehicle_delete_body),
    ) {
        PhotoStrip(stringResource(R.string.vehicle_photo), listOfNotNull(photo), onChange = { photo = it.lastOrNull() }, single = true)
        TextInput(stringResource(R.string.vehicle_name), name, { name = it.take(40) }, words = true, placeholder = stringResource(R.string.vehicle_name_hint))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(stringResource(R.string.vehicle_make), make, { make = it.take(40) }, Modifier.weight(1f), words = true)
            TextInput(stringResource(R.string.vehicle_model), model, { model = it.take(40) }, Modifier.weight(1f), words = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberInput(stringResource(R.string.vehicle_year), year, { year = it }, Modifier.weight(1f), decimals = false)
            NumberInput(stringResource(R.string.vehicle_odometer), odo, { odo = it }, Modifier.weight(1f), suffix = fmt.distanceUnit)
        }
        ChoiceInput(stringResource(R.string.vehicle_engine), EngineType.entries.map { it to engineLabel(it) }, engine) { engine = it }
        if (isNew && engine.isTwoStroke) {
            SwitchInput(stringResource(R.string.vehicle_template), template, stringResource(R.string.vehicle_template_sub)) { template = it }
        }
        if (engine == EngineType.TWO_STROKE_PREMIX) {
            FieldHeading(stringResource(R.string.premix_title))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInput(stringResource(R.string.vehicle_ratio), ratio, { ratio = it }, Modifier.weight(1f), suffix = ":1", decimals = false)
                TextInput(stringResource(R.string.vehicle_oil), oil, { oil = it.take(40) }, Modifier.weight(1f), words = true)
            }
        }
        FieldHeading(stringResource(R.string.vehicle_papers))
        TextInput(stringResource(R.string.vehicle_plate), plate, { plate = it.take(20).uppercase() })
        TextInput(stringResource(R.string.vehicle_vin), vin, { vin = it.take(40).uppercase() })
        FieldNote(stringResource(R.string.vehicle_vin_note))
        FieldHeading(stringResource(R.string.vehicle_purchase))
        DateInput(stringResource(R.string.vehicle_purchase_date), purchaseDate, { purchaseDate = it }, optional = true)
        NumberInput(stringResource(R.string.vehicle_purchase_price), price, { price = it }, suffix = fmt.currencySymbol)
        TextInput(stringResource(R.string.notes), notes, { notes = it.take(2000) }, singleLine = false, minLines = 3, sentences = true)
    }
}
