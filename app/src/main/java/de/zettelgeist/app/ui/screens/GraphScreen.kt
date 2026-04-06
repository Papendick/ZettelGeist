package de.zettelgeist.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.data.model.Note
import de.zettelgeist.app.data.model.NoteLink
import de.zettelgeist.app.ui.components.EmptyState
import de.zettelgeist.app.ui.theme.InboxAmber
import de.zettelgeist.app.ui.theme.SourceBlue
import de.zettelgeist.app.ui.theme.ZettelEmerald
import de.zettelgeist.app.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlin.math.sqrt

data class GraphNode(
    val noteId: String,
    val label: String,
    val workspace: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val linkCount: Int
)

@Composable
fun GraphScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()

    var links by remember { mutableStateOf<List<NoteLink>>(emptyList()) }
    var nodes by remember { mutableStateOf<List<GraphNode>>(emptyList()) }
    var isSimulating by remember { mutableStateOf(false) }

    LaunchedEffect(allNotes) {
        val allLinks = try {
            viewModel.getAllLinksOnce()
        } catch (_: Exception) { emptyList() }
        links = allLinks

        // Count links per note
        val linkCounts = mutableMapOf<String, Int>()
        allLinks.forEach { link ->
            linkCounts[link.sourceNoteId] = (linkCounts[link.sourceNoteId] ?: 0) + 1
            link.targetNoteId?.let { linkCounts[it] = (linkCounts[it] ?: 0) + 1 }
        }

        val width = 800f
        val height = 1200f
        nodes = allNotes.mapIndexed { index, note ->
            val angle = (index.toFloat() / allNotes.size.coerceAtLeast(1)) * 2 * Math.PI
            val radius = 200f
            GraphNode(
                noteId = note.id,
                label = note.title.take(15),
                workspace = note.workspace,
                x = width / 2 + (radius * Math.cos(angle)).toFloat(),
                y = height / 2 + (radius * Math.sin(angle)).toFloat(),
                linkCount = linkCounts[note.id] ?: 0
            )
        }
        isSimulating = true
    }

    // Force-directed simulation
    LaunchedEffect(isSimulating) {
        if (!isSimulating || nodes.size < 2) return@LaunchedEffect
        val nodeMap = nodes.associateBy { it.noteId }

        repeat(100) {
            val updated = nodes.toMutableList()

            // Repulsion between all nodes
            for (i in updated.indices) {
                for (j in i + 1 until updated.size) {
                    val dx = updated[j].x - updated[i].x
                    val dy = updated[j].y - updated[i].y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val force = 5000f / (dist * dist)
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    updated[i] = updated[i].copy(vx = updated[i].vx - fx, vy = updated[i].vy - fy)
                    updated[j] = updated[j].copy(vx = updated[j].vx + fx, vy = updated[j].vy + fy)
                }
            }

            // Attraction along links
            links.forEach { link ->
                val source = updated.indexOfFirst { it.noteId == link.sourceNoteId }
                val target = link.targetNoteId?.let { tid -> updated.indexOfFirst { it.noteId == tid } } ?: -1
                if (source >= 0 && target >= 0) {
                    val dx = updated[target].x - updated[source].x
                    val dy = updated[target].y - updated[source].y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val force = dist * 0.01f
                    val fx = (dx / dist) * force
                    val fy = (dy / dist) * force
                    updated[source] = updated[source].copy(vx = updated[source].vx + fx, vy = updated[source].vy + fy)
                    updated[target] = updated[target].copy(vx = updated[target].vx - fx, vy = updated[target].vy - fy)
                }
            }

            // Center gravity
            val centerX = 400f
            val centerY = 600f
            for (i in updated.indices) {
                val dx = centerX - updated[i].x
                val dy = centerY - updated[i].y
                updated[i] = updated[i].copy(
                    vx = updated[i].vx + dx * 0.001f,
                    vy = updated[i].vy + dy * 0.001f
                )
            }

            // Apply velocity with damping
            for (i in updated.indices) {
                updated[i] = updated[i].copy(
                    x = updated[i].x + updated[i].vx * 0.8f,
                    y = updated[i].y + updated[i].vy * 0.8f,
                    vx = updated[i].vx * 0.9f,
                    vy = updated[i].vy * 0.9f
                )
            }

            nodes = updated
            delay(16)
        }
        isSimulating = false
    }

    if (allNotes.size < 3) {
        EmptyState(
            emoji = "\uD83D\uDD78\uFE0F",
            title = "Noch nicht genug Notizen",
            subtitle = "Erstelle mindestens 3 Notizen mit Wikilinks,\num den Graphen zu sehen."
        )
    } else {
        val density = LocalDensity.current

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTapGestures { offset ->
                        val tapped = nodes.find { node ->
                            val dx = offset.x - node.x * density.density
                            val dy = offset.y - node.y * density.density
                            sqrt(dx * dx + dy * dy) < 30f * density.density
                        }
                        tapped?.let { onNoteClick(it.noteId) }
                    }
                }
        ) {
            val scaleX = size.width / 800f
            val scaleY = size.height / 1200f
            val nodeMap = nodes.associateBy { it.noteId }

            // Draw edges
            links.forEach { link ->
                val source = nodeMap[link.sourceNoteId]
                val target = link.targetNoteId?.let { nodeMap[it] }
                if (source != null && target != null) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(source.x * scaleX, source.y * scaleY),
                        end = Offset(target.x * scaleX, target.y * scaleY),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Draw nodes
            nodes.forEach { node ->
                val color = when (node.workspace) {
                    "inbox" -> InboxAmber
                    "source" -> SourceBlue
                    "zettel" -> ZettelEmerald
                    else -> Color.Gray
                }
                val radius = (8f + node.linkCount * 3f).coerceAtMost(24f)

                drawCircle(
                    color = color,
                    radius = radius * scaleX,
                    center = Offset(node.x * scaleX, node.y * scaleY)
                )

                // Draw label
                drawContext.canvas.nativeCanvas.drawText(
                    node.label,
                    node.x * scaleX,
                    node.y * scaleY - radius * scaleX - 8f,
                    android.graphics.Paint().apply {
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                        this.color = android.graphics.Color.GRAY
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}
