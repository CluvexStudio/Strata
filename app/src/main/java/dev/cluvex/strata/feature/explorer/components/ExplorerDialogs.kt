package dev.cluvex.strata.feature.explorer.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import dev.cluvex.strata.R
import dev.cluvex.strata.core.model.SortBy
import dev.cluvex.strata.core.model.SortOrder
import dev.cluvex.strata.ui.expressive.Art
import dev.cluvex.strata.ui.expressive.ArtTone
import dev.cluvex.strata.ui.expressive.ExpressiveDialog
import dev.cluvex.strata.ui.expressive.HeroShapes

enum class InputSelection {
    END,

    ALL,

    BASE_NAME,
}

/** Suffixes offered as one-tap chips when a new file is created. */
val NewFileSuffixes = listOf(
    ".txt", ".md", ".json", ".xml", ".yaml", ".ini", ".log", ".csv", ".sh", ".py", ".js", ".html", ".css",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmText: String = stringResource(R.string.common_ok),
    select: InputSelection = InputSelection.END,
    ltr: Boolean = false,
    suffixes: List<String> = emptyList(),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, selection = selectionFor(initial, select))) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    val confirm = { if (value.text.isNotBlank()) onConfirm(value.text.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    textStyle = if (ltr) LocalTextStyle.current.copy(textDirection = TextDirection.Ltr) else LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.focusRequester(focus),
                )
                if (suffixes.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (suffix in suffixes) {
                            val typed = value.text.trimEnd()
                            FilterChip(
                                selected = typed.length > suffix.length && typed.endsWith(suffix, ignoreCase = true),
                                onClick = {
                                    val next = withSuffix(typed, suffix)
                                    value = TextFieldValue(next, selection = TextRange(next.length))
                                },
                                label = { Text(suffix) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = value.text.isNotBlank()) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Swaps a trailing extension for [suffix], or appends it when the name does not have one yet. */
internal fun withSuffix(name: String, suffix: String): String {
    val trimmed = name.trimEnd()
    if (trimmed.isEmpty()) return suffix.trimStart('.')
    if (trimmed.endsWith('.')) return trimmed + suffix.trimStart('.')
    val dot = trimmed.lastIndexOf('.')
    val tail = if (dot > 0) trimmed.substring(dot + 1) else ""
    val hasExtension = tail.isNotEmpty() && tail.length <= 12 && tail.all { it.isLetterOrDigit() }
    return (if (hasExtension) trimmed.substring(0, dot) else trimmed) + suffix
}

internal fun selectionFor(text: String, select: InputSelection): TextRange = when (select) {
    InputSelection.END -> TextRange(text.length)
    InputSelection.ALL -> TextRange(0, text.length)
    InputSelection.BASE_NAME -> TextRange(0, baseNameEnd(text))
}

internal fun baseNameEnd(name: String): Int {
    val dot = name.lastIndexOf('.')
    return if (dot > 0) dot else name.length
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.common_ok),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
fun DeleteDialog(
    count: Int,
    recycleByDefault: Boolean,
    onConfirm: (toRecycleBin: Boolean) -> Unit,
    onDismiss: () -> Unit,
    allowRecycle: Boolean = true,
) {
    var checked by remember { mutableStateOf(recycleByDefault) }
    val toBin = allowRecycle && checked
    ExpressiveDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.common_delete),
        art = if (toBin) Art.EMPTY_BIN else null,
        icon = if (toBin) null else Icons.Rounded.DeleteForever,
        tone = if (toBin) ArtTone.PRIMARY else ArtTone.ERROR,
        shapes = if (toBin) HeroShapes.calm else HeroShapes.danger,
        text = {
            Column {
                Text(pluralStringResource(R.plurals.explorer_delete_confirm, count, count))
                if (allowRecycle) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = checked, onValueChange = { checked = it })
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = null)
                        Text(stringResource(R.string.explorer_move_to_bin_option), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (!toBin) {
                    Text(
                        stringResource(R.string.explorer_delete_warning),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(toBin) }) {
                Text(
                    stringResource(if (toBin) R.string.explorer_move_to_bin else R.string.common_delete),
                    color = if (toBin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    current: SortOrder,
    showHidden: Boolean,
    onChange: (SortOrder) -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    thisFolderOnly: Boolean? = null,
    folderName: String = "",
    onThisFolderOnly: (Boolean) -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.explorer_sort_by), style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortBy.entries.forEach { by ->
                    FilterChip(
                        selected = current.by == by,
                        onClick = { onChange(current.copy(by = by)) },
                        label = { Text(stringResource(by.labelRes())) },
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !current.descending,
                    onClick = { onChange(current.copy(descending = false)) },
                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                    label = { Text(stringResource(R.string.explorer_ascending)) },
                )
                FilterChip(
                    selected = current.descending,
                    onClick = { onChange(current.copy(descending = true)) },
                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                    label = { Text(stringResource(R.string.explorer_descending)) },
                )
            }

            ToggleRow(stringResource(R.string.explorer_folders_first), current.dirsFirst) { onChange(current.copy(dirsFirst = it)) }
            ToggleRow(stringResource(R.string.common_show_hidden), showHidden) { onToggleHidden(it) }
            if (thisFolderOnly != null) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                ToggleRow(
                    if (folderName.isBlank()) stringResource(R.string.explorer_only_this_folder) else stringResource(R.string.explorer_only_this_folder_named, folderName),
                    thisFolderOnly,
                    tag = "sort-this-folder",
                ) { onThisFolderOnly(it) }
                Text(
                    stringResource(if (thisFolderOnly) R.string.explorer_sort_own_on else R.string.explorer_sort_own_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, tag: String? = null, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onChange)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@StringRes
private fun SortBy.labelRes(): Int = when (this) {
    SortBy.NAME -> R.string.common_name
    SortBy.SIZE -> R.string.common_size
    SortBy.DATE -> R.string.common_date
    SortBy.TYPE -> R.string.common_type
}
