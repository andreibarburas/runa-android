package com.brbrs.runa.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Horizontal tag row with an inline text field.
 * Type a tag name and press Enter/Done to add it.
 * Tap the × on a chip to remove it.
 */
@Composable
fun TagInputRow(
    tags: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }

    fun commit() {
        val tag = input.trim().lowercase().replace(" ", "-")
        if (tag.isNotBlank() && !tags.contains(tag)) onTagAdded(tag)
        input = ""
    }

    Column(modifier = modifier) {
        // Existing tags
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick  = {},
                        label    = { Text("#$tag", style = MaterialTheme.typography.labelLarge) },
                        trailingIcon = {
                            IconButton(
                                onClick  = { onTagRemoved(tag) },
                                modifier = Modifier.size(18.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = InputChipDefaults.inputChipColors(
                            containerColor      = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor          = MaterialTheme.colorScheme.primary,
                            trailingIconColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            enabled       = true,
                            selected      = false,
                            borderColor   = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // Tag input field
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.LocalOffer,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            TextField(
                value         = input,
                onValueChange = { input = it },
                placeholder   = {
                    Text(
                        "Add a tag…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                colors        = TextFieldDefaults.colors(
                    focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    cursorColor             = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
