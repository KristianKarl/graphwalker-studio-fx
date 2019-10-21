package org.graphwalker.layout

import org.graphwalker.model.EdgeFX
import org.graphwalker.model.GraphFX
import org.graphwalker.model.VertexFX
import org.slf4j.LoggerFactory
import java.util.*

class RandomLayoutFX(internal var graph: GraphFX) : LayoutFX() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal var rnd = Random()

    override fun execute() {
        logger.debug("Running random layout")
        for (element in graph.getElements()) {
            if (element is VertexFX) {
                val x = rnd.nextDouble() * graph.rootPane.width
                val y = rnd.nextDouble() * graph.rootPane.height
                element.relocate(x, y)
            }
        }
    }

    override fun doEdges() {}

    override fun doEdge(edge: EdgeFX) {
    }
}
