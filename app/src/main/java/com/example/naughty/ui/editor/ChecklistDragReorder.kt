package com.example.naughty.ui.editor

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot

data class BlockLayoutBounds(
    val top: Float,
    val height: Float
) {
    val bottom: Float get() = top + height
    val centerY: Float get() = top + height / 2f
}

fun canReorderChecklists(blocks: List<EditorBlock>): Boolean {
    return blocks.count { it is EditorBlock.Checklist } >= 2
}

fun reorderChecklistBlock(
    blocks: List<EditorBlock>,
    fromIndex: Int,
    toIndex: Int
): List<EditorBlock>? {
    if (!canReorderChecklists(blocks)) return null
    if (fromIndex == toIndex) return null
    if (fromIndex !in blocks.indices || toIndex !in blocks.indices) return null
    if (blocks[fromIndex] !is EditorBlock.Checklist || blocks[toIndex] !is EditorBlock.Checklist) {
        return null
    }
    val mutable = blocks.toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

fun findTargetChecklistSwapIndex(
    blocks: List<EditorBlock>,
    currentIndex: Int,
    draggedCenterY: Float,
    boundsById: Map<String, BlockLayoutBounds>
): Int? {
    if (!canReorderChecklists(blocks)) return null
    if (currentIndex !in blocks.indices) return null
    if (blocks[currentIndex] !is EditorBlock.Checklist) return null

    var nextIndex = -1
    for (i in (currentIndex + 1) until blocks.size) {
        if (blocks[i] is EditorBlock.Checklist) {
            nextIndex = i
            break
        }
    }
    if (nextIndex != -1) {
        val nextBounds = boundsById[blocks[nextIndex].id]
        if (nextBounds != null && draggedCenterY > nextBounds.centerY) {
            return nextIndex
        }
    }

    var prevIndex = -1
    for (i in (currentIndex - 1) downTo 0) {
        if (blocks[i] is EditorBlock.Checklist) {
            prevIndex = i
            break
        }
    }
    if (prevIndex != -1) {
        val prevBounds = boundsById[blocks[prevIndex].id]
        if (prevBounds != null && draggedCenterY < prevBounds.centerY) {
            return prevIndex
        }
    }

    return null
}

@Stable
class ChecklistDragReorderState(
    private val getBlocks: () -> List<EditorBlock>,
    private val onBeginReorder: () -> Unit,
    private val onMoveChecklist: (Int, Int) -> Boolean,
    private val onFinishReorder: () -> Unit,
    private val onSwapHaptic: () -> Unit
) {
    var draggingBlockId by mutableStateOf<String?>(null)
        private set

    val isDragging: Boolean
        get() = draggingBlockId != null

    private var draggedVisualTop by mutableFloatStateOf(0f)
    private var grabOffsetFromBlockTop: Float = 0f
    private var lastPointerRootY: Float = 0f
    private var containerCoords: LayoutCoordinates? = null
    private var viewportTopInRoot: Float = 0f
    private var viewportBottomInRoot: Float = 0f
    private val blockBounds = mutableStateMapOf<String, BlockLayoutBounds>()
    private var moveGeneration: Int = 0
    private var placedGeneration: Int = 0

    fun onContainerPositioned(coords: LayoutCoordinates) {
        containerCoords = coords
    }

    fun onViewportPositioned(coords: LayoutCoordinates) {
        val top = coords.positionInRoot().y
        viewportTopInRoot = top
        viewportBottomInRoot = top + coords.size.height.toFloat()
    }

    fun onBlockPlaced(blockId: String, top: Float, height: Float) {
        blockBounds[blockId] = BlockLayoutBounds(top = top, height = height)
        if (blockId == draggingBlockId) {
            placedGeneration = moveGeneration
        }
    }

    fun pruneMissingBlocks(validIds: Set<String>) {
        val staleIds = blockBounds.keys.filter { it !in validIds }
        for (id in staleIds) {
            blockBounds.remove(id)
        }
        val activeId = draggingBlockId
        if (activeId != null && activeId !in validIds) {
            endDrag()
        }
    }

    fun startDrag(blockId: String, pointerRootY: Float) {
        val blocks = getBlocks()
        if (!canReorderChecklists(blocks)) return
        val container = containerCoords?.takeIf { it.isAttached } ?: return
        val bounds = blockBounds[blockId] ?: return

        val pointerYInContainer = pointerRootY - container.positionInRoot().y
        draggingBlockId = blockId
        lastPointerRootY = pointerRootY
        grabOffsetFromBlockTop = pointerYInContainer - bounds.top
        draggedVisualTop = bounds.top
        moveGeneration = 0
        placedGeneration = 0
        onBeginReorder()
    }

    fun updateDrag(pointerRootY: Float) {
        val activeId = draggingBlockId ?: return
        val blocks = getBlocks()
        if (!canReorderChecklists(blocks)) {
            endDrag()
            return
        }
        val container = containerCoords?.takeIf { it.isAttached } ?: return
        val activeBounds = blockBounds[activeId] ?: return

        lastPointerRootY = pointerRootY
        val pointerYInContainer = pointerRootY - container.positionInRoot().y
        val rawTop = pointerYInContainer - grabOffsetFromBlockTop

        val checklistBounds = blocks.mapNotNull { block ->
            if (block is EditorBlock.Checklist) blockBounds[block.id] else null
        }
        draggedVisualTop = if (checklistBounds.size >= 2) {
            val minTop = checklistBounds.minOf { it.top } - 12f
            val maxBottom = checklistBounds.maxOf { it.bottom }
            val maxTop = (maxBottom - activeBounds.height + 12f).coerceAtLeast(minTop)
            rawTop.coerceIn(minTop, maxTop)
        } else {
            rawTop
        }

        if (placedGeneration == moveGeneration) {
            val currentIndex = blocks.indexOfFirst { it.id == activeId }
            if (currentIndex != -1) {
                val draggedCenterY = draggedVisualTop + activeBounds.height / 2f
                val targetIndex = findTargetChecklistSwapIndex(
                    blocks = blocks,
                    currentIndex = currentIndex,
                    draggedCenterY = draggedCenterY,
                    boundsById = blockBounds
                )
                if (targetIndex != null && onMoveChecklist(currentIndex, targetIndex)) {
                    moveGeneration++
                    onSwapHaptic()
                }
            }
        }
    }

    fun refreshAfterScroll() {
        if (isDragging) {
            updateDrag(lastPointerRootY)
        }
    }

    fun computeAutoScrollStep(
        edgeThresholdPx: Float,
        maxStepPx: Float = 18f
    ): Float {
        if (!isDragging || edgeThresholdPx <= 0f || viewportBottomInRoot <= viewportTopInRoot) {
            return 0f
        }
        val topZoneEnd = viewportTopInRoot + edgeThresholdPx
        val bottomZoneStart = viewportBottomInRoot - edgeThresholdPx
        return when {
            lastPointerRootY < topZoneEnd -> {
                val fraction = ((topZoneEnd - lastPointerRootY) / edgeThresholdPx).coerceIn(0f, 1f)
                -maxStepPx * fraction
            }
            lastPointerRootY > bottomZoneStart -> {
                val fraction = ((lastPointerRootY - bottomZoneStart) / edgeThresholdPx).coerceIn(0f, 1f)
                maxStepPx * fraction
            }
            else -> 0f
        }
    }

    fun translationYFor(blockId: String): Float {
        if (draggingBlockId != blockId) return 0f
        val layoutTop = blockBounds[blockId]?.top ?: return 0f
        return draggedVisualTop - layoutTop
    }

    fun endDrag() {
        if (draggingBlockId != null) {
            draggingBlockId = null
            draggedVisualTop = 0f
            grabOffsetFromBlockTop = 0f
            moveGeneration = 0
            placedGeneration = 0
            onFinishReorder()
        }
    }
}

@Composable
fun rememberChecklistDragReorderState(
    blocks: List<EditorBlock>,
    onBeginReorder: () -> Unit,
    onMoveChecklist: (Int, Int) -> Boolean,
    onFinishReorder: () -> Unit,
    onSwapHaptic: () -> Unit
): ChecklistDragReorderState {
    val currentBlocks by rememberUpdatedState(blocks)
    val currentOnBegin by rememberUpdatedState(onBeginReorder)
    val currentOnMove by rememberUpdatedState(onMoveChecklist)
    val currentOnFinish by rememberUpdatedState(onFinishReorder)
    val currentOnSwapHaptic by rememberUpdatedState(onSwapHaptic)

    return remember {
        ChecklistDragReorderState(
            getBlocks = { currentBlocks },
            onBeginReorder = { currentOnBegin() },
            onMoveChecklist = { from, to -> currentOnMove(from, to) },
            onFinishReorder = { currentOnFinish() },
            onSwapHaptic = { currentOnSwapHaptic() }
        )
    }
}

internal fun performChecklistPickupHaptic(view: View) {
    val constant = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.DRAG_START
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.GESTURE_START
        else -> HapticFeedbackConstants.CLOCK_TICK
    }
    view.performHapticFeedback(constant)
}

internal fun performChecklistReorderTickHaptic(view: View) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HapticFeedbackConstants.SEGMENT_TICK
    } else {
        HapticFeedbackConstants.CLOCK_TICK
    }
    view.performHapticFeedback(constant)
}
