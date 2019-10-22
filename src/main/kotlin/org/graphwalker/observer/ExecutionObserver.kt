package org.graphwalker.observer

import javafx.scene.control.Tab
import javafx.scene.paint.Color
import org.graphwalker.core.event.EventType
import org.graphwalker.core.event.Observer
import org.graphwalker.core.machine.Machine
import org.graphwalker.core.model.Element
import org.graphwalker.core.model.Vertex
import org.graphwalker.model.EdgeFX
import org.graphwalker.model.VertexFX
import org.graphwalker.views.ModelEditor
import org.slf4j.LoggerFactory

class ExecutionObserver(tabs : List<Tab>) : Observer {
    private val tabs = tabs
    private val logger = LoggerFactory.getLogger(this::class.java)


    override fun update(machine: Machine, element: Element, eventType: EventType?) {
        if (EventType.BEFORE_ELEMENT == eventType) {

            if (element is Vertex.RuntimeVertex) {
                logger.debug("Element is (vertex) : " + element.name)
                var vertexFX = getVertexFX(element)
                vertexFX.rect.fill = Color.GREEN
            } else {
                logger.debug("Element is (edge) : " + element.name)
                var edgeFX = getEdgeFX(element)
                edgeFX.path.stroke = Color.GREEN
            }
        }
    }

    fun getVertexFX(element: Element) : VertexFX {
        for (tab in tabs) {
            val modelEditor = tab.content as ModelEditor
            for (v in modelEditor.vertices) {
                if (v.element.id == element.id) {
                    return v
                }
            }
        }
        throw IllegalArgumentException("Did not find vertex in model: " + element.id)
    }

    fun getEdgeFX(element: Element) : EdgeFX {
        for (tab in tabs) {
            val modelEditor = tab.content as ModelEditor
            for (e in modelEditor.edges) {
                if (e.element.id == element.id) {
                    return e
                }
            }
        }
        throw IllegalArgumentException("Did not find edge in model: " + element.id)
    }
}