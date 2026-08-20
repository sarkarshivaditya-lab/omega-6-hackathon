package com.udc.collection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminPinDialog(
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
    verifyPin: suspend (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Lock, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Admin Access Required", fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Enter the admin PIN to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) { pin = it; error = false } },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) ({ Text("Incorrect PIN. Try again.", color = MaterialTheme.colorScheme.error) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isVerifying = true
                        if (verifyPin(pin)) onVerified()
                        else { error = true; pin = "" }
                        isVerifying = false
                    }
                },
                enabled = pin.isNotEmpty() && !isVerifying
            ) { Text(if (isVerifying) "Verifying…" else "Verify") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangePinDialog(
    onSave: (newPin: String) -> Unit,
    onDismiss: () -> Unit,
    verifyCurrentPin: suspend (String) -> Boolean
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var currentError by remember { mutableStateOf(false) }
    var mismatchError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Admin PIN", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 8) { currentPin = it; currentError = false } },
                    label = { Text("Current PIN") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    isError = currentError,
                    supportingText = if (currentError) ({ Text("Incorrect current PIN", color = MaterialTheme.colorScheme.error) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 8) { newPin = it; mismatchError = false } },
                    label = { Text("New PIN (4–8 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 8) { confirmPin = it; mismatchError = false } },
                    label = { Text("Confirm New PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = mismatchError,
                    supportingText = if (mismatchError) ({ Text("PINs do not match", color = MaterialTheme.colorScheme.error) }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        when {
                            newPin != confirmPin -> mismatchError = true
                            newPin.length < 4 -> mismatchError = true
                            else -> {
                                isVerifying = true
                                if (!verifyCurrentPin(currentPin)) {
                                    currentError = true
                                } else {
                                    onSave(newPin)
                                }
                                isVerifying = false
                            }
                        }
                    }
                },
                enabled = currentPin.isNotEmpty() && newPin.isNotEmpty() && confirmPin.isNotEmpty() && !isVerifying
            ) { Text(if (isVerifying) "Verifying…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) { Text("Cancel") }
        }
    )
}
