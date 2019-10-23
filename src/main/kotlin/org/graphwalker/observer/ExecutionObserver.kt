package org.graphwalker.observer

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
import tornadofx.*

class ProgressEvent(val completed: Double) : FXEvent()

class ExecutionObserver(modelEditors: MutableCollection<ModelEditor>) : Observer, Controller() {
    private val modelEditors = modelEditors
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var lastTimeCheck = System.currentTimeMillis()
    private val interval = 200L


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
            if (System.currentTimeMillis() - lastTimeCheck > interval) {
                logger.debug("ProgressEvent fired")
                fire(ProgressEvent(machine.currentContext.pathGenerator.stopCondition.fulfilment))
                lastTimeCheck = System.currentTimeMillis()
            }
        }
    }

    fun getVertexFX(element: Element): VertexFX {
        for (modelEditor in modelEditors) {
            for (v in modelEditor.vertices) {
                if (v.jsonVertex.vertex.id == element.id) {
                    return v
                }
            }
        }
        throw IllegalArgumentException("Did not find vertex in model: " + element.id)
    }

    fun getEdgeFX(element: Element): EdgeFX {
        for (modelEditor in modelEditors) {
            for (e in modelEditor.edges) {
                if (e.jsonEdge.edge.id == element.id) {
                    return e
                }
            }
        }
        throw IllegalArgumentException("Did not find edge in model: " + element.id)
    }
}