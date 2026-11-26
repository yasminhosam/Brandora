package com.yasminhosam.brandora.feature.workinghours.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WorkingHoursScreen(
    state: WorkingHoursUiState,
    onEvent: (WorkingHoursEvent) -> Unit,
    onBackClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.primary, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back),
                    tint = scheme.onPrimary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.working_hours_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimary
                )
                Text(
                    stringResource(R.string.working_hours_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimary.copy(alpha = 0.85f)
                )
            }
            if (state.isOnboarding) {
                TextButton(onClick = onSkipClick) {
                    Text(stringResource(R.string.working_hours_skip), color = scheme.onPrimary)
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.errorRes != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                Text(
                    stringResource(state.errorRes),
                    color = scheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                TextButton(onClick = { onEvent(WorkingHoursEvent.RetryClicked) }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(state.days, key = { it.day }) { day ->
                        DayCard(
                            day = day,
                            hasError = day.day in state.invalidDays,
                            locale = locale,
                            onEvent = onEvent
                        )
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (state.saveErrorRes != null) {
                        Text(
                            stringResource(state.saveErrorRes),
                            color = scheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = { onEvent(WorkingHoursEvent.SaveClicked) },
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.working_hours_save))
                        }
                    }
                }
            }
        }
    }

    state.pickerTarget?.let { target ->
        val day = state.days.firstOrNull { it.day == target.day }
        val initial = when (target.field) {
            TimeField.OPENS -> day?.opensAt
            TimeField.CLOSES -> day?.closesAt
        } ?: LocalTime.of(9, 0)
        TimePickerDialog(
            initial = initial,
            onConfirm = { onEvent(WorkingHoursEvent.TimePicked(it)) },
            onDismiss = { onEvent(WorkingHoursEvent.PickerDismissed) }
        )
    }
}

@Composable
private fun DayCard(
    day: WorkingDay,
    hasError: Boolean,
    locale: Locale,
    onEvent: (WorkingHoursEvent) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    day.day.getDisplayName(TextStyle.FULL, locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(
                        if (day.isOpen) R.string.working_hours_open else R.string.working_hours_closed
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Switch(
                checked = day.isOpen,
                onCheckedChange = { onEvent(WorkingHoursEvent.DayToggled(day.day, it)) }
            )
        }
        if (day.isOpen) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TimeButton(
                    labelRes = R.string.working_hours_opens_at,
                    time = day.opensAt,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                ) { onEvent(WorkingHoursEvent.TimeClicked(day.day, TimeField.OPENS)) }
                TimeButton(
                    labelRes = R.string.working_hours_closes_at,
                    time = day.closesAt,
                    locale = locale,
                    modifier = Modifier.weight(1f)
                ) { onEvent(WorkingHoursEvent.TimeClicked(day.day, TimeField.CLOSES)) }
            }
            if (hasError) {
                Text(
                    stringResource(R.string.working_hours_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeButton(
    labelRes: Int,
    time: LocalTime?,
    locale: Locale,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val text = time?.let {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(it)
    } ?: "--:--"
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 8.dp
        )
        ) {
        Text(stringResource(labelRes, text), maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = is24Hour
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}