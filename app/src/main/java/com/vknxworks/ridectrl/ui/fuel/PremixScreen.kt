package com.vknxworks.ridectrl.ui.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.ridectrl.R
import com.vknxworks.ridectrl.container
import com.vknxworks.ridectrl.data.EngineType
import com.vknxworks.ridectrl.data.fuelFor
import com.vknxworks.ridectrl.data.oilFor
import com.vknxworks.ridectrl.data.parseInt
import com.vknxworks.ridectrl.data.parseNumber
import com.vknxworks.ridectrl.ui.components.ChoiceInput
import com.vknxworks.ridectrl.ui.components.CtrlHeroCard
import com.vknxworks.ridectrl.ui.components.CtrlSecondaryButton
import com.vknxworks.ridectrl.ui.components.FieldNote
import com.vknxworks.ridectrl.ui.components.ScreenTopBar
import com.vknxworks.ridectrl.ui.components.SectionLabel
import com.vknxworks.ridectrl.ui.components.TextInput
import com.vknxworks.ridectrl.ui.components.ctrlFieldColors
import com.vknxworks.ridectrl.ui.components.rememberFmt
import com.vknxworks.ridectrl.ui.theme.CtrlColors
import com.vknxworks.ridectrl.ui.theme.CtrlShapes
import com.vknxworks.ridectrl.ui.theme.CtrlType
import com.vknxworks.ridectrl.ui.theme.Gutter

private val PRESETS = listOf(25, 32, 40, 50)

/**
 * The premix calculator, made for the pump: big numbers and big targets. Fuel to oil, or (reverse) how
 * much fuel the oil you have is good for. It starts from the vehicle's own ratio and remembers it.
 */
@Composable
fun PremixScreen(vehicleId: String?) {
    val c = LocalContext.current.container
    val fmt = rememberFmt()
    val data by c.logs.data.collectAsStateWithLifecycle()
    val premixVehicles = data.vehicles.filter { it.engine == EngineType.TWO_STROKE_PREMIX }
    var vid by rememberSaveable { mutableStateOf(vehicleId ?: premixVehicles.firstOrNull()?.id) }
    val vehicle = data.vehicles.firstOrNull { it.id == vid }

    var reverse by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var ratio by rememberSaveable { mutableStateOf((vehicle?.premixRatio ?: 40).toString()) }
    var oilBrand by rememberSaveable { mutableStateOf(vehicle?.oilBrand ?: "") }

    val r = parseInt(ratio)?.takeIf { it in 10..200 }
    val a = parseNumber(amount)
    val result: String = when {
        a == null || r == null -> "–"
        !reverse -> fmt.num(oilFor(fmt.userToLitres(a), r.toDouble()), 0)
        else -> fmt.num(fmt.litresToUser(fuelFor(a, r.toDouble())), 2)
    }
    val savedRatio = vehicle?.premixRatio
    val changed = vehicle != null && (r != savedRatio || oilBrand.trim() != vehicle.oilBrand)

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenTopBar(stringResource(R.string.premix_title))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Gutter).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (premixVehicles.size > 1 || (vehicle == null && premixVehicles.isNotEmpty())) {
                ChoiceInput(stringResource(R.string.vehicle), premixVehicles.map { it.id as String? to it.name }, vid) { picked ->
                    vid = picked
                    data.vehicles.firstOrNull { it.id == picked }?.let { v ->
                        ratio = (v.premixRatio ?: 40).toString()
                        oilBrand = v.oilBrand
                    }
                }
            }

            // Ratio presets
            SectionLabel(stringResource(R.string.premix_ratio), Modifier.padding(start = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { p ->
                    val on = r == p
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .clip(CtrlShapes.Control)
                            .background(if (on) CtrlColors.Accent else CtrlColors.Surface)
                            .border(1.dp, if (on) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Control)
                            .clickable { ratio = p.toString() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$p:1", style = CtrlType.Key.copy(fontSize = 22.sp), color = if (on) CtrlColors.OnAccent else CtrlColors.Text)
                    }
                }
            }
            BigField(stringResource(R.string.premix_custom_ratio), ratio, { if (it.length <= 3 && it.all(Char::isDigit)) ratio = it }, ":1", decimals = false)

            // Input
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    stringResource(if (reverse) R.string.premix_oil_you_have else R.string.premix_fuel_to_mix),
                    Modifier.weight(1f).padding(start = 4.dp),
                )
                CtrlSecondaryButton(onClick = { reverse = !reverse; amount = "" }) {
                    androidx.compose.material3.Icon(Icons.Outlined.SwapVert, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.premix_reverse))
                }
            }
            BigField(
                stringResource(if (reverse) R.string.premix_oil_label else R.string.premix_fuel_label, fmt.volumeUnit),
                amount,
                { if (it.length <= 8 && it.all { ch -> ch.isDigit() || ch == ',' || ch == '.' }) amount = it },
                if (reverse) "ml" else fmt.volumeUnit,
            )

            // Result
            CtrlHeroCard(
                label = stringResource(if (reverse) R.string.premix_fuel_result else R.string.premix_oil_to_add),
                meta = r?.let { "$it:1" },
            ) {
                Text(
                    if (reverse) "$result ${fmt.volumeUnit}" else stringResource(R.string.premix_ml, result),
                    style = CtrlType.Hero.copy(fontSize = 72.sp),
                    maxLines = 1,
                )
                if (oilBrand.isNotBlank() && !reverse) Text(oilBrand, style = MaterialTheme.typography.titleMedium)
            }

            if (vehicle != null) {
                TextInput(stringResource(R.string.vehicle_oil), oilBrand, { oilBrand = it.take(40) }, words = true)
                if (changed) {
                    CtrlSecondaryButton(
                        onClick = {
                            val id = vehicle.id
                            c.logs.update { d -> d.copy(vehicles = d.vehicles.map { if (it.id == id) it.copy(premixRatio = r, oilBrand = oilBrand.trim()) else it }) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.premix_save_default, vehicle.name)) }
                }
            }
            FieldNote(stringResource(R.string.premix_note))
        }
    }
}

/** A large, glove-friendly number field. */
@Composable
private fun BigField(label: String, value: String, onChange: (String) -> Unit, suffix: String, decimals: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = CtrlType.Readout.copy(textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold),
        suffix = { Text(suffix, style = MaterialTheme.typography.titleLarge, color = CtrlColors.Muted) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number),
        shape = CtrlShapes.Field,
        colors = ctrlFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
