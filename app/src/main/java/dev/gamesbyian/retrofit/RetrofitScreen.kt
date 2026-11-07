package dev.gamesbyian.retrofit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val actions = listOf("add", "replace", "remove", "clean", "modify")

@Composable
fun RetrofitScreen(repository: RoomRepository) {
    var draft by remember { mutableStateOf<RoomDraft?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var movingId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var savedDraft by remember { mutableStateOf<RoomDraft?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var retrySave by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val restored = repository.load()
            savedDraft = restored
            draft = restored
        } catch (error: Exception) {
            message = "Saved draft could not be loaded: " + (error.message ?: "unknown error")
        } finally {
            initialized = true
        }
    }
    LaunchedEffect(draft, retrySave) {
        val snapshot = draft ?: return@LaunchedEffect
        if (savedDraft == snapshot) return@LaunchedEffect
        saveError = null
        try {
            repository.save(snapshot)
            savedDraft = snapshot
        } catch (cancel: CancellationException) {
            // Rapid edits cancel superseded saves; this is not a storage failure.
            throw cancel
        } catch (error: Exception) {
            saveError = "Changes could not be saved: " + (error.message ?: "storage error")
        }
    }
    LaunchedEffect(draft?.id) {
        photo = null
        val snapshot = draft ?: return@LaunchedEffect
        try {
            photo = repository.loadPreview(snapshot)?.asImageBitmap()
        } catch (error: Exception) {
            message = "Stored photo is unreadable: " + (error.message ?: "unknown error")
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val newDraft = repository.importPhoto(uri)
                    // Only replace the in-memory draft after the new source is fully accepted.
                    savedDraft = newDraft
                    draft = newDraft
                    saveError = null
                    selectedId = null
                    message = null
                } catch (error: Exception) {
                    message = "Photo not imported: " + (error.message ?: "unsupported photo")
                }
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Retrofit", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    if (initialized && draft == null) {
                        Button(onClick = {
                            picker.launch(PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            ))
                        }) { Text("Choose photo") }
                    }
                }
                if (message != null) {
                    SelectionContainer {
                        Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
                if (!initialized) {
                    CircularProgressIndicator()
                } else {
                    val room = draft
                    if (room == null) {
                        Text("Choose an equipment-room photograph to begin a private local draft.")
                        Text("No photos or instructions are sent to an AI service in this version.")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (saveError == null) {
                                Text(
                                    if (savedDraft == room) "Saved locally" else "Saving…",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                Text(
                                    "Not saved",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(onClick = { retrySave++ }) { Text("Retry saving") }
                            }
                        }
                        if (saveError != null) {
                            Text(
                                saveError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            "Tap the photo to add A, B, C…  Pinch to zoom. Select a marker to edit or move it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 100.dp)
                        ) {
                            val image = photo
                            if (image != null) {
                                PhotoWorkspace(
                                    draft = room, image = image, selectedId = selectedId,
                                    onSelect = { selectedId = it; movingId = null },
                                    onTap = { point ->
                                        val move = movingId
                                        if (move != null) {
                                            draft = room.copy(markers = room.markers.map {
                                                if (it.id == move) it.copy(x = point.x, y = point.y)
                                                else it
                                            })
                                            movingId = null
                                        } else if (room.markers.size < 200) {
                                            val updated = room.mark(point)
                                            draft = updated
                                            selectedId = updated.markers.last().id
                                        } else {
                                            message = "This room has reached the 200-marker limit"
                                        }
                                    }
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        if (room.markers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                room.markers.forEach { marker ->
                                    OutlinedButton(onClick = {
                                        selectedId = marker.id
                                        movingId = null
                                    }) {
                                        Text(marker.label + " · " + marker.action)
                                    }
                                }
                            }
                        }
                        val selected = room.markers.firstOrNull { it.id == selectedId }
                        if (selected != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Marker " + selected.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { movingId = selected.id }) {
                                    Text(if (movingId == selected.id) "Tap new spot" else "Move")
                                }
                                TextButton(onClick = {
                                    draft = room.copy(
                                        markers = room.markers.filterNot { it.id == selected.id }
                                    )
                                    selectedId = null
                                    movingId = null
                                }) { Text("Remove marker") }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                actions.forEach { action ->
                                    FilterChip(
                                        selected = selected.action == action,
                                        onClick = {
                                            draft = room.copy(markers = room.markers.map {
                                                if (it.id == selected.id) it.copy(action = action)
                                                else it
                                            })
                                        },
                                        label = { Text(action.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = selected.detail,
                                onValueChange = { value ->
                                    if (value.length <= 2000) {
                                        draft = room.copy(markers = room.markers.map {
                                            if (it.id == selected.id) it.copy(detail = value)
                                            else it
                                        })
                                    }
                                },
                                label = { Text("Change at " + selected.label) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        }
                        OutlinedTextField(
                            value = room.instructions,
                            onValueChange = { if (it.length <= 12000) draft = room.copy(instructions = it) },
                            label = { Text("Overall instructions") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        Text(
                            "Local draft only. Equipment catalog and reviewed AYA sharing follow.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoWorkspace(
    draft: RoomDraft,
    image: ImageBitmap,
    selectedId: String?,
    onTap: (UnitPoint) -> Unit,
    onSelect: (String) -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember(draft.id) { mutableFloatStateOf(1f) }
    var pan by remember(draft.id) { mutableStateOf(Offset.Zero) }
    val markerRadius = with(LocalDensity.current) { 24.dp.toPx() }
    val gestureState = rememberTransformableState { magnify, travel, _ ->
        zoom = (zoom * magnify).coerceIn(1f, 4f)
        val limitX = size.width * (zoom - 1f) / 2f
        val limitY = size.height * (zoom - 1f) / 2f
        pan = if (zoom == 1f) Offset.Zero else Offset(
            (pan.x + travel.x).coerceIn(-limitX, limitX),
            (pan.y + travel.y).coerceIn(-limitY, limitY)
        )
    }
    Box(
        modifier = Modifier.fillMaxSize().clipToBounds().background(Color(0xFF242A2F))
            .onSizeChanged { size = it }
            .transformable(state = gestureState)
    ) {
        if (size.width > 0 && size.height > 0) {
            val bounds = fitPhoto(
                size.width.toFloat(), size.height.toFloat(), draft.width, draft.height
            )
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                }
            ) {
                Image(
                    bitmap = image,
                    contentDescription = "Equipment-room photograph",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().pointerInput(draft, size) {
                        detectTapGestures { tap ->
                            toUnitPoint(tap.x, tap.y, bounds)?.let(onTap)
                        }
                    }
                )
                draft.markers.forEach { marker ->
                    val (x, y) = toViewPoint(UnitPoint(marker.x, marker.y), bounds)
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (x - markerRadius).roundToInt(),
                                    (y - markerRadius).roundToInt()
                                )
                            }
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedId == marker.id) Color(0xFF69DBAE)
                                else Color(0xFFFFCB71)
                            )
                            .clickable { onSelect(marker.id) }
                            .semantics {
                                contentDescription = "Marker " + marker.label +
                                    ", " + marker.action + ", tap to edit"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(marker.label, color = Color(0xFF17242C))
                    }
                }
            }
        }
    }
}
