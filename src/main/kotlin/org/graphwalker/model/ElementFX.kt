package org.graphwalker.model

interface ElementFX {

    val elementId: String

    val isSelected: Boolean

    val isVisited: Boolean

    val elementName: String

    fun setStartElement(setElement: Boolean)

    fun highlight(highLight: Boolean)

    fun visited(visit: Boolean)

    fun selected(select: Boolean)
}