package org.graphwalker.layout

import javafx.geometry.Point2D
import javafx.scene.shape.Rectangle
import org.graphwalker.model.EdgeFX
import org.graphwalker.model.GraphFX
import org.graphwalker.model.VertexFX
import org.slf4j.LoggerFactory
import java.util.*

class GraphWalkerLayoutFX : LayoutFX {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal var graph: GraphFX? = null
    internal var rnd = Random()
    internal var locatedRectangles: HashMap<VertexFX, Rectangle> = HashMap()
    internal var edges: HashMap<Map<VertexFX, VertexFX>, Int> = HashMap()
    private val MINIMAL_DISTANCE = 20.0
    private val CONTROL_PT_DISTANCE = 100.0

    constructor() {
        this.graph = null
    }

    constructor(graph: GraphFX) {
        this.graph = graph
    }

    override fun execute() {
        logger.debug("Running the GraphWalker layout")
        for (element in graph!!.getElements()) {
            if (element is VertexFX) {
                placeVertex(element)
            }
        }
        for (v in locatedRectangles.keys) {
            val r = locatedRectangles[v]
            v.relocate(r.getLayoutX() + 1e6,
                    r.getLayoutY() + 1e6)
        }

        doEdges()
    }

    override fun doEdges() {
        logger.debug("Running the GraphWalker edge layout")
        for (element in graph!!.getElements()) {
            if (element is EdgeFX) {
                placeEdges(element)
            }
        }
    }

    fun doEdge(edge: EdgeFX) {
        placeEdges(edge)
    }

    private fun placeEdges(edge: EdgeFX) {
        var instances: Int? = 1
        val e = Hashtable<VertexFX, VertexFX>()
        e.put(edge.source, edge.target)
        if (edges.containsKey(e)) {
            instances = edges[e] + 1
            edges[e] = instances
        } else {
            edges[e] = instances
        }

        val p3: Point2D
        val p4: Point2D
        val startPt = Point2D(edge.source.x.getValue(), edge.source.y.getValue())
        val endPt = Point2D(edge.target.x.getValue(), edge.target.y.getValue())

        if (edge.source === edge.target) {
            p3 = Point2D(startPt.x + CONTROL_PT_DISTANCE * instances!!,
                    startPt.y)
            p4 = Point2D(startPt.x,
                    startPt.y + CONTROL_PT_DISTANCE * instances)
        } else {
            val angle = calcRotationAngleInDegrees(startPt, endPt)

            p3 = Point2D(endPt.x + CONTROL_PT_DISTANCE * instances!!.toDouble() * Math.sin(angle + 90),
                    endPt.y + CONTROL_PT_DISTANCE * instances.toDouble() * Math.cos(angle + 90))
            p4 = Point2D(startPt.x + CONTROL_PT_DISTANCE * instances.toDouble() * Math.sin(angle + 90),
                    startPt.y + CONTROL_PT_DISTANCE * instances.toDouble() * Math.cos(angle + 90))
        }

        edge.curve.setControlX1(p4.x)
        edge.curve.setControlY1(p4.y)
        edge.curve.setControlX2(p3.x)
        edge.curve.setControlY2(p3.y)
        edge.update()
    }

    private fun placeVertex(vertex: VertexFX) {
        if (locatedRectangles.containsKey(vertex)) {
            return
        }
        val rectangle = Rectangle(vertex.layoutX - MINIMAL_DISTANCE,
                vertex.layoutY - MINIMAL_DISTANCE,
                vertex.layoutBounds.width + MINIMAL_DISTANCE,
                vertex.layoutBounds.height + MINIMAL_DISTANCE)

        if (locatedRectangles.size < 1) {
            val pt = Point2D(rnd.nextDouble() * graph!!.rootPane.width, rnd.nextDouble() * graph!!.rootPane.height)
            rectangle.relocate(pt.x, pt.y)
            locatedRectangles[vertex] = rectangle
            return
        }

        for (v in locatedRectangles.keys) {
            val r = locatedRectangles[v]
            do {
                val pt = Point2D(rnd.nextDouble() * graph!!.rootPane.width, rnd.nextDouble() * graph!!.rootPane.height)
                rectangle.relocate(pt.x, pt.y)
                logger.debug("Testing pt: $pt")
            } while (rectangle.boundsInParent.intersects(r.getBoundsInParent()))
        }

        locatedRectangles[vertex] = rectangle
    }

    companion object {

        /**
         * Calculates the angle from centerPt to targetPt in degrees.
         * The return should range from [0,360), rotating CLOCKWISE,
         * 0 and 360 degrees represents NORTH,
         * 90 degrees represents EAST, etc...
         *
         *
         * Assumes all points are in the same coordinate space.  If they are not,
         * you will need to call SwingUtilities.convertPointToScreen or equivalent
         * on all arguments before passing them  to this function.
         *
         * @param centerPt Point we are rotating around.
         * @param targetPt Point we want to calcuate the angle to.
         * @return angle in degrees.  This is the angle from centerPt to targetPt.
         */
        fun calcRotationAngleInDegrees(centerPt: Point2D, targetPt: Point2D): Double {
            // calculate the angle theta from the deltaY and deltaX values
            // (atan2 returns radians values from [-PI,PI])
            // 0 currently points EAST.
            // NOTE: By preserving Y and X param order to atan2,  we are expecting
            // a CLOCKWISE angle direction.
            val theta = Math.atan2(targetPt.y - centerPt.y, targetPt.x - centerPt.x)

            // rotate the theta angle clockwise by 90 degrees
            // (this makes 0 point NORTH)
            // NOTE: adding to an angle rotates it clockwise.
            // subtracting would rotate it counter-clockwise
            //theta += Math.PI/2.0;

            // convert from radians to degrees
            // this will give you an angle from [0->270],[-180,0]
            var angle = Math.toDegrees(theta)

            // convert to positive range [0-360)
            // since we want to prevent negative angles, adjust them now.
            // we can assume that atan2 will not return a negative value
            // greater than one partial rotation
            if (angle < 0) {
                angle += 360.0
            }

            return angle
        }
    }
}
