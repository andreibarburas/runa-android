package com.brbrs.runa.ui.screens.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brbrs.runa.data.repository.JournalEntry
import com.brbrs.runa.data.repository.JournalRepository
import com.brbrs.runa.ui.theme.DarkPrimary
import com.brbrs.runa.ui.theme.LocalIsDark
import com.brbrs.runa.ui.theme.runaBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

// ── Dark mode colour filter ───────────────────────────────────────────────────
// Applied to MAPNIK tiles in dark mode to invert luminance and desaturate,
// targeting OSM water (#AAD3DF) → #262626 and land (#F2EFE9) → #090909.
// Passes null in light mode — TilesOverlay.INVERT_COLORS_NONE was removed.

private val WARM_DARK_FILTER: ColorMatrixColorFilter by lazy {
    val s = 0.7416f
    val cr = -s * 0.299f; val cg = -s * 0.587f; val cb = -s * 0.114f
    val bias = s * 255f
    val matrix = ColorMatrix(
        floatArrayOf(
            cr, cg, cb, 0f, bias,
            cr, cg, cb, 0f, bias,
            cr, cg, cb, 0f, bias,
             0f, 0f, 0f, 1f,   0f,
        )
    )
    ColorMatrixColorFilter(matrix)
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MapViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
) : ViewModel() {
    val entries: StateFlow<List<JournalEntry>> = journalRepository
        .getEntriesWithLocation()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MapScreen(
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val isDark  = LocalIsDark.current
    val entries by viewModel.entries.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ))
        }
    }

    val markerArgb = DarkPrimary.toArgb()

    Column(
        modifier = modifier
            .fillMaxSize()
            .runaBackground(isDark)
            .statusBarsPadding(),
    ) {
        Text(
            text     = "Map",
            style    = MaterialTheme.typography.headlineLarge,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
        ) {
            if (entries.isEmpty()) {
                EmptyMapState()
            } else {
                OsmMapView(
                    entries       = entries,
                    isDark        = isDark,
                    markerArgb    = markerArgb,
                    onMarkerClick = onEntryClick,
                    modifier      = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ── OsmMapView ────────────────────────────────────────────────────────────────

@Composable
private fun OsmMapView(
    entries: List<JournalEntry>,
    isDark: Boolean,
    markerArgb: Int,
    onMarkerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasFitBounds   = remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        factory  = { ctx ->
            Configuration.getInstance().userAgentValue = "Runa-Android-App/1.0"

            MapView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(
                    if (entries.isNotEmpty())
                        GeoPoint(entries.first().latitude!!, entries.first().longitude!!)
                    else
                        GeoPoint(52.3676, 4.9041)
                )
                // Apply dark filter immediately on construction if needed
                overlayManager.tilesOverlay.setColorFilter(
                    if (isDark) WARM_DARK_FILTER else null
                )
            }
        },
        update   = { mapView ->
            // Single tile source for both modes — filter handles dark appearance
            if (mapView.tileProvider.tileSource.name() != TileSourceFactory.MAPNIK.name()) {
                mapView.setTileSource(TileSourceFactory.MAPNIK)
            }

            // Update dark filter on theme change
            mapView.overlayManager.tilesOverlay.setColorFilter(
                if (isDark) WARM_DARK_FILTER else null
            )

            mapView.overlays.clear()

            for (entry in entries) {
                val marker = Marker(mapView)
                marker.position = GeoPoint(entry.latitude!!, entry.longitude!!)
                marker.title    = entry.title.ifBlank { "Entry" }
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon     = createMarkerDrawable(markerArgb)
                marker.setOnMarkerClickListener { _, _ ->
                    onMarkerClick(entry.id)
                    true
                }
                mapView.overlays.add(marker)
            }
            mapView.invalidate()

            if (!hasFitBounds.value && entries.isNotEmpty()) {
                hasFitBounds.value = true
                fitToEntries(mapView, entries)
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> Unit
                Lifecycle.Event.ON_RESUME -> Unit
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fitToEntries(mapView: MapView, entries: List<JournalEntry>) {
    if (entries.size == 1) {
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(GeoPoint(entries.first().latitude!!, entries.first().longitude!!))
        return
    }
    val lats = entries.mapNotNull { it.latitude }
    val lons = entries.mapNotNull { it.longitude }
    val bbox = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
    if (mapView.width > 0 && mapView.height > 0) {
        mapView.zoomToBoundingBox(bbox, false, 64)
    } else {
        mapView.addOnFirstLayoutListener { view, _, _, _, _ ->
            (view as MapView).zoomToBoundingBox(bbox, false, 64)
        }
    }
}

private fun createMarkerDrawable(color: Int): android.graphics.drawable.Drawable {
    val drawable = GradientDrawable()
    drawable.shape = GradientDrawable.OVAL
    drawable.setColor(color)
    drawable.setStroke(4, AndroidColor.WHITE)
    drawable.setSize(64, 64)
    drawable.setBounds(0, 0, 64, 64)
    return drawable
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyMapState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            Icon(
                Icons.Outlined.LocationOff,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp),
            )
            Text(
                "No locations yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Add a location when writing an entry and it will appear here.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
