package org.graphwalker.util

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import org.graphwalker.model.EdgeFX
import org.graphwalker.model.GraphFX
import org.graphwalker.model.VertexFX
import org.json.JSONObject
import org.slf4j.LoggerFactory

class MouseGestures(internal var graph: GraphFX) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    internal val dragContext = DragContext()

    internal var onMousePressedEventHandler: EventHandler<MouseEvent> = EventHandler { event ->
        logger.debug("Clicked")

        // If control modifier key is pressed, ignore
        if (event.isControlDown) {
            return@EventHandler
        }

        val scale = graph.scale
        val node = event.source as Node
        dragContext.x = node.boundsInParent.minX * scale - event.screenX
        dragContext.y = node.boundsInParent.minY * scale - event.screenY
    }

    internal var onMouseDraggedEventHandler: EventHandler<MouseEvent> = EventHandler { event ->
        if (event.isShiftDown) {
            return@EventHandler
        }
        val node = event.source as Node

        var offsetX = event.screenX + dragContext.x
        var offsetY = event.screenY + dragContext.y

        // adjust the offset in case we are zoomed
        val scale = graph.scale

        offsetX /= scale
        offsetY /= scale

        node.relocate(offsetX, offsetY)
        event.consume()
    }

    internal var setOnMouseEntered = { event ->
        val node = event.getSource() as Node
        //node.setEffect(new DropShadow());

        if (!logger.isDebugEnabled) {
            return
        }
        val jsonPosition = JSONObject()
        val jsonNode = JSONObject()
        jsonNode.append("Position", jsonPosition)

        if (node is VertexFX) {
            jsonNode.put("Type", "Vertex")
            jsonNode.put("Name", node.vertex.getName())
            jsonNode.put("Id", node.vertex.getId())
            jsonNode.put("x", node.x.getValue())
            jsonNode.put("y", node.y.getValue())
        } else if (node is EdgeFX) {
            jsonNode.put("Type", "Edge")
            jsonNode.put("Name", node.edge.name)
            jsonNode.put("Id", node.edge.id)
        } else {
            jsonNode.put("Type", "Unknown")
        }

        logger.debug("Entered: " + jsonNode.toString(2))
    }

    internal var setOnMouseExited = { event ->
        val node = event.getSource() as Node
        node.effect = null
        if (node is VertexFX) {
            logger.debug("Exited: " + node.vertex.getName())
        } else if (node is EdgeFX) {
            logger.debug("Exited: " + node.edge.name)
        }
    }

    fun makeDraggable(node: Node) {
        node.onMousePressed = onMousePressedEventHandler
        node.onMouseDragged = onMouseDraggedEventHandler
    }

    fun logHover(node: Node) {
        node.setOnMouseEntered(setOnMouseEntered)
        node.setOnMouseExited(setOnMouseExited)
    }

    internal inner class DragContext {

        var x: Double = 0.toDouble()
        var y: Double = 0.toDouble()
    }
}
