package org.graphwalker.layout

import org.graphwalker.model.EdgeFX

abstract class LayoutFX {

    abstract fun execute()

    abstract fun doEdges()

    abstract fun doEdge(edge: EdgeFX)
}
