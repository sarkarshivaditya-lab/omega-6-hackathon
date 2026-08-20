package com.udc.collection.ui.screen.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.udc.collection.domain.model.Gender
import com.udc.collection.util.ValidationUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step1PatientDetailsScreen(
    state: WizardState,
    vm: PatientWizardViewModel,
    onNext: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var showErrors by remember { mutableStateOf(false) }

    val nameError = if (showErrors) ValidationUtils.validatePatientName(state.name) else null
    val phoneError = if (state.phone.isNotBlank()) ValidationUtils.validatePhone(state.phone) else null
    val ageError = if (state.age.isNotBlank()) ValidationUtils.validateAge(state.age) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = vm::updateName,
            label = { Text("Patient Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.age,
                onValueChange = vm::updateAge,
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = ageError != null,
                supportingText = ageError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            GenderDropdown(
                selected = state.gender,
                onSelect = vm::updateGender,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.phone,
            onValueChange = vm::updatePhone,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        AutocompleteField(
            value = state.address,
            onValueChange = vm::updateAddress,
            label = "Address",
            suggestions = state.recentAddresses
        )

        AutocompleteField(
            value = state.referringDoctor,
            onValueChange = vm::updateReferringDoctor,
            label = "Referring Doctor",
            suggestions = state.recentDoctors
        )

        OutlinedTextField(
            value = state.date.format(DateTimeFormatter.ofPattern("dd / MM / yyyy", Locale.ENGLISH)),
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { Icon(Icons.Filled.CalendarToday, null) }
        )

        OutlinedTextField(
            value = state.remarks,
            onValueChange = vm::updateRemarks,
            label = { Text("Remarks") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                showErrors = true
                val hasNameErr = ValidationUtils.validatePatientName(state.name) != null
                val hasPhoneErr = state.phone.isNotBlank() && ValidationUtils.validatePhone(state.phone) != null
                val hasAgeErr = state.age.isNotBlank() && ValidationUtils.validateAge(state.age) != null
                if (!hasNameErr && !hasPhoneErr && !hasAgeErr) onNext()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next: Select Tests", style = MaterialTheme.typography.titleSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GenderDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.ifBlank { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Gender.entries.forEach { g ->
                DropdownMenuItem(
                    text = { Text(g.label) },
                    onClick = { onSelect(g.label); expanded = false }
                )
            }
        }
    }
}

@Composable
internal fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>
) {
    var showSuggestions by remember { mutableStateOf(false) }
    val filtered = remember(value, suggestions) {
        if (value.isBlank()) emptyList()
        else suggestions.filter { it.contains(value, ignoreCase = true) && it != value }.take(5)
    }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); showSuggestions = it.isNotBlank() },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
        if (showSuggestions && filtered.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                filtered.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(suggestion); showSuggestions = false }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
