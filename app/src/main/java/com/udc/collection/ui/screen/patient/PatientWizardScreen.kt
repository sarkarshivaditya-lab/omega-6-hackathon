package com.udc.collection.ui.screen.patient

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.ui.components.UDCTopBar

private val STEP_LABELS = listOf("Patient Details", "Select Tests", "Billing")

@Composable
fun PatientWizardScreen(
    onBack: () -> Unit,
    onSaved: (patientId: Long) -> Unit,
    viewModel: PatientWizardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val allTests by viewModel.filteredTests.collectAsState()
    val allPackages by viewModel.filteredPackages.collectAsState()
    val frequentTests by viewModel.frequentTests.collectAsState()

    val hasUnsavedData = state.name.isNotBlank() || state.selectedTests.isNotEmpty()
    var showDiscardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = hasUnsavedData || state.currentStep > 0) {
        when {
            state.currentStep > 0 -> viewModel.previousStep()
            hasUnsavedData -> showDiscardDialog = true
            else -> onBack()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?", fontWeight = FontWeight.SemiBold) },
            text = { Text("You have unsaved patient data. Leaving now will discard it.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.dismissDraft()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    // Draft restore dialog
    if (state.showDraftRestorePrompt) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDraft,
            title = { Text("Restore Draft?", fontWeight = FontWeight.SemiBold) },
            text = { Text("You have an unfinished patient entry. Would you like to continue where you left off?") },
            confirmButton = {
                Button(onClick = viewModel::restoreDraft) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDraft) { Text("Start Fresh") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                UDCTopBar(
                    title = STEP_LABELS[state.currentStep],
                    onBack = {
                        when {
                            state.currentStep > 0 -> viewModel.previousStep()
                            hasUnsavedData -> showDiscardDialog = true
                            else -> onBack()
                        }
                    }
                )
                WizardStepIndicator(
                    currentStep = state.currentStep,
                    totalSteps = 3,
                    labels = STEP_LABELS
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (state.currentStep) {
                0 -> Step1PatientDetailsScreen(
                    state = state,
                    vm = viewModel,
                    onNext = viewModel::nextStep
                )
                1 -> Step2TestSelectionScreen(
                    state = state,
                    vm = viewModel,
                    allTests = allTests,
                    allPackages = allPackages,
                    frequentTests = frequentTests,
                    onNext = viewModel::nextStep,
                    onBack = viewModel::previousStep
                )
                2 -> Step3BillingScreen(
                    state = state,
                    vm = viewModel,
                    onBack = viewModel::previousStep,
                    onSave = {
                        viewModel.savePatient { patientId -> onSaved(patientId) }
                    }
                )
            }
        }
    }
}

@Composable
private fun WizardStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    labels: List<String>
) {
    Column {
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = "${index + 1}. $label",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == currentStep) MaterialTheme.colorScheme.primary
                    else if (index < currentStep) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
