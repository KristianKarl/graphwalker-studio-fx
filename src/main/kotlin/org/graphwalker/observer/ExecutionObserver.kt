package org.graphwalker.observer

import org.graphwalker.core.event.EventType
import org.graphwalker.core.event.Observer
import org.graphwalker.core.machine.Context
import org.graphwalker.core.machine.Machine
import org.graphwalker.core.model.Element
import org.graphwalker.core.model.Vertex
import org.slf4j.LoggerFactory

class ExecutionObserver : Observer {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private var lastElement: Element? = null
    private lateinit var lastContext: Context

    override fun update(machine: Machine, element: Element, eventType: EventType?) {
        if (EventType.BEFORE_ELEMENT == eventType) {

            if (lastElement != null) {
                if (lastElement is Vertex.RuntimeVertex) {
                    logger.debug("Last context: " + lastContext + ", last element (vertex): " + lastElement)
                } else {
                    logger.debug("Last context: " + lastContext + ", last element (edge): " + lastElement)
                }
            }

            if (element is Vertex.RuntimeVertex) {
                logger.debug("Element is (vertex) : " + element)
            } else {
                logger.debug("Element is (edge) : " + element)
            }
            lastElement = element
            lastContext = machine.currentContext
        }
    }

}