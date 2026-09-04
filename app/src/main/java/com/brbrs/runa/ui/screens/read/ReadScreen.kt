package com.brbrs.runa.ui.screens.read

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.SubcomposeAsyncImage
import com.brbrs.runa.data.repository.JournalEntry
import com.brbrs.runa.data.repository.JournalRepository
import com.brbrs.runa.ui.theme.*
import com.brbrs.runa.ui.theme.DMSerifDisplayFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// ── Grouped list item types ───────────────────────────────────────────────────

sealed class ReadItem {
    data class YearHeader(val year: String)                                                          : ReadItem()
    data class MonthHeader(val year: String, val month: String)                                      : ReadItem()
    data class DayHeader(val year: String, val month: String, val day: String, val weekday: String)  : ReadItem()
    data class Entry(val entry: JournalEntry)                                                        : ReadItem()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ReadViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
) : ViewModel() {

    private val _searchQuery  = MutableStateFlow("")
    private val _activeTag    = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String>   = _searchQuery.asStateFlow()
    val activeTag:   StateFlow<String?>  = _activeTag.asStateFlow()

    /** All tags across all entries — for the filter row. */
    val allTags: StateFlow<List<String>> = journalRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawEntries: StateFlow<List<JournalEntry>> = combine(
        _searchQuery.debounce(300),
        _activeTag,
    ) { query, tag -> Pair(query, tag) }
        .flatMapLatest { (query, tag) ->
            when {
                query.isNotBlank() -> journalRepository.searchEntries(query)
                tag  != null       -> journalRepository.getEntriesByTag(tag)
                else               -> journalRepository.getAllEntries()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readItems: StateFlow<List<ReadItem>> = combine(
        rawEntries,
        _searchQuery,
        _activeTag,
    ) { entries, query, tag ->
        if (query.isNotBlank() || tag != null) {
            // Flat list when filtering
            entries.map { ReadItem.Entry(it) }
        } else {
            buildGroupedList(entries)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(q: String) {
        _searchQuery.update { q }
        if (q.isNotBlank()) _activeTag.value = null   // search clears tag filter
    }

    fun onTagSelected(tag: String) {
        _activeTag.value = if (_activeTag.value == tag) null else tag
        _searchQuery.value = ""   // tag filter clears search
    }

    fun clearTagFilter() { _activeTag.value = null }

    private fun buildGroupedList(entries: List<JournalEntry>): List<ReadItem> {
        val items     = mutableListOf<ReadItem>()
        var lastYear  = ""
        var lastMonth = ""
        var lastDay   = ""

        for (entry in entries) {
            val cal     = Calendar.getInstance().apply { timeInMillis = entry.entryDateTimeMs }
            val year    = "%04d".format(cal.get(Calendar.YEAR))
            val month   = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
            val day     = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
            val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)

            if (year != lastYear) {
                items.add(ReadItem.YearHeader(year))
                lastYear = year; lastMonth = ""; lastDay = ""
            }
            if (month != lastMonth) {
                items.add(ReadItem.MonthHeader(year, month))
                lastMonth = month; lastDay = ""
            }
            if (day != lastDay) {
                items.add(ReadItem.DayHeader(year, month, day, weekday))
                lastDay = day
            }
            items.add(ReadItem.Entry(entry))
        }
        return items
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ReadScreen(
    onEntryClick: (String) -> Unit,
    viewModel: ReadViewModel = hiltViewModel(),
) {
    val isDark      = LocalIsDark.current
    val readItems   by viewModel.readItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags     by viewModel.allTags.collectAsState()
    val activeTag   by viewModel.activeTag.collectAsState()

    Box(modifier = Modifier.fillMaxSize().runaBackground(isDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "Read",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value           = searchQuery,
                onValueChange   = viewModel::onSearchQueryChanged,
                placeholder     = { Text("Search entries…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon     = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedTextColor        = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor      = MaterialTheme.colorScheme.onBackground,
                    cursorColor             = MaterialTheme.colorScheme.primary,
                ),
            )

            // Tag filter row — only shown when there are tags
            if (allTags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    allTags.forEach { tag ->
                        val isSelected = activeTag == tag
                        FilterChip(
                            selected = isSelected,
                            onClick  = { viewModel.onTagSelected(tag) },
                            label    = { Text("#$tag", style = MaterialTheme.typography.labelLarge) },
                            shape    = RoundedCornerShape(8.dp),
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor     = MaterialTheme.colorScheme.primary,
                                containerColor         = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor             = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled       = true,
                                selected      = isSelected,
                                borderColor         = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (readItems.isEmpty()) {
                EmptyState(hasFilter = searchQuery.isNotBlank() || activeTag != null)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding      = PaddingValues(bottom = 100.dp),
                ) {
                    items(readItems, key = { item ->
                        when (item) {
                            is ReadItem.YearHeader  -> "year-${item.year}"
                            is ReadItem.MonthHeader -> "month-${item.year}-${item.month}"
                            is ReadItem.DayHeader   -> "day-${item.year}-${item.month}-${item.day}"
                            is ReadItem.Entry       -> item.entry.id
                        }
                    }) { item ->
                        when (item) {
                            is ReadItem.YearHeader  -> YearHeader(item.year)
                            is ReadItem.MonthHeader -> MonthHeader(item.month)
                            is ReadItem.DayHeader   -> DayHeader(item.day, item.weekday)
                            is ReadItem.Entry       -> {
                                EntryListItem(
                                    entry   = item.entry,
                                    isDark  = isDark,
                                    onClick = { onEntryClick(item.entry.id) },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Header composables ────────────────────────────────────────────────────────

@Composable
private fun YearHeader(year: String) {
    Text(
        text     = year,
        style    = MaterialTheme.typography.headlineMedium.copy(fontFamily = DMSerifDisplayFamily),
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        text     = month,
        style    = MaterialTheme.typography.titleLarge.copy(fontFamily = DMSerifDisplayFamily),
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 2.dp),
    )
}

@Composable
private fun DayHeader(day: String, weekday: String) {
    Row(
        modifier              = Modifier.padding(top = 12.dp, bottom = 6.dp, start = 2.dp),
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(day,     style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(weekday, style = MaterialTheme.typography.bodySmall,   color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 1.dp))
    }
}

// ── Entry list item ───────────────────────────────────────────────────────────

@Composable
private fun EntryListItem(
    entry: JournalEntry,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val timeLabel = remember(entry.entryDateTimeMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.entryDateTimeMs))
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .runaCard(isDark, cornerRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            if (entry.title.isNotBlank()) {
                Text(
                    text     = entry.title,
                    style    = MaterialTheme.typography.titleMedium.copy(fontFamily = DMSerifDisplayFamily),
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            if (entry.body.isNotBlank()) {
                Text(
                    text     = entry.body,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (entry.title.isBlank()) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
            }
            // Tags
            if (entry.tags.isNotEmpty()) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    entry.tags.take(3).forEach { tag ->
                        Text(
                            text  = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    }
                    if (entry.tags.size > 3) {
                        Text(
                            text  = "+${entry.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text  = timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )
        }

        val firstPhoto = entry.photoPaths.firstOrNull()
        if (firstPhoto != null) {
            SubcomposeAsyncImage(
                model              = File(firstPhoto),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(hasFilter: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.AutoStories,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Text(
                if (hasFilter) "No entries match" else "No entries yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (hasFilter) "Try a different search or tag" else "Start writing your first memory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
