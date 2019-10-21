package org.graphwalker.util

import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.transform.Scale
import org.graphwalker.model.VertexFX
import org.slf4j.LoggerFactory

class ZoomableScrollPaneFX(internal var content: Node) : ScrollPane() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal var zoomGroup: Group
    internal var contentGroup: Group
    internal var scaleTransform: Scale
    var scaleValue = 1.0
        internal set
    internal var delta = 0.1

    init {
        contentGroup = Group()
        zoomGroup = Group()
        contentGroup.children.add(zoomGroup)
        zoomGroup.children.add(content)
        setContent(contentGroup)

        scaleTransform = Scale(scaleValue, scaleValue, 0.0, 0.0)
        zoomGroup.transforms.add(scaleTransform)

        zoomGroup.onScroll = ZoomHandler()

        isPannable = true
        vvalue = 0.5
        hvalue = 0.5
        hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
    }

    fun zoomToActual() {
        zoomTo(1.0)
    }

    fun zoomTo(scaleValue: Double) {
        logger.debug("Scale to: $scaleValue")
        if (scaleValue > 0.01) {
            this.scaleValue = scaleValue
        } else {
            this.scaleValue = 0.01
        }

        val v = vvalue
        val h = hvalue
        logger.debug("v, h: $v, $h")
        scaleTransform.x = this.scaleValue
        scaleTransform.y = this.scaleValue
        vvalue = v
        hvalue = h
    }

    fun zoomActual() {
        scaleValue = 1.0
        zoomTo(scaleValue)
    }

    fun zoomOut() {
        scaleValue -= delta
        if (java.lang.Double.compare(scaleValue, 0.1) < 0) {
            scaleValue = 0.1
        }
        zoomTo(scaleValue)
    }

    fun zoomIn() {
        scaleValue += delta
        if (java.lang.Double.compare(scaleValue, 10.0) > 0) {
            scaleValue = 10.0
        }
        zoomTo(scaleValue)

    }

    fun zoomToFit() {

        var left = 0.0
        var right = 0.0
        var upper = 0.0
        var lower = 0.0

        var isFirstVertexDone = false
        for (node in (content as Pane).children) {
            if (node is VertexFX) {
                if (isFirstVertexDone) {
                    if (node.getBoundsInParent().minX < left) {
                        left = node.getBoundsInParent().minX
                    }
                    if (node.getBoundsInParent().maxX > right) {
                        right = node.getBoundsInParent().maxX
                    }
                    if (node.getBoundsInParent().minY < upper) {
                        upper = node.getBoundsInParent().minY
                    }
                    if (node.getBoundsInParent().maxY > lower) {
                        lower = node.getBoundsInParent().maxY
                    }
                } else {
                    isFirstVertexDone = true
                    left = node.getBoundsInParent().minX
                    right = node.getBoundsInParent().maxX
                    upper = node.getBoundsInParent().minY
                    lower = node.getBoundsInParent().maxY
                }
            }
        }
        val width = right - left
        val height = lower - upper

        if (width <= 0 || height <= 0) {
            logger.debug("No vertices, no scrolling nor scaling")
            return
        }

        val scaleX = viewportBounds.width / width
        val scaleY = viewportBounds.height / height
        val scale = Math.min(scaleX, scaleY)

        val h = (left + width / 2) / 1e6
        val v = (upper + height / 2) / 1e6
        logger.debug("h, v: $h, $v")

        vvalue = v
        hvalue = h
        zoomTo(scale)
    }

    private inner class ZoomHandler : EventHandler<ScrollEvent> {

        override fun handle(scrollEvent: ScrollEvent) {
            if (scrollEvent.deltaY < 0) {
                scaleValue -= delta
            } else {
                scaleValue += delta
            }

            zoomTo(scaleValue)
            scrollEvent.consume()
        }
    }
}
