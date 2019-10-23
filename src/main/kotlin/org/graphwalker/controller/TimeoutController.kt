package org.graphwalker.controller

import javafx.animation.Animation.INDEFINITE
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.property.SimpleLongProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.util.Duration
import tornadofx.*

const val TIMEOUT_CHECK_INTERVAL = 500L
const val TIMEOUTVAL = 15000L

object TIMEOUT_EVENT : FXEvent()
object CLOSE_ALL_EVENT : FXEvent()
object START_TIMER_EVENT : FXEvent()
object HEARTBEAT_EVENT : FXEvent()

class TimeoutController : Controller() {

    val timeoutAccural = SimpleLongProperty()

    private var timeline: Timeline? = null

    init {
        subscribe<START_TIMER_EVENT> { startTimer() }
        subscribe<HEARTBEAT_EVENT> { resetTimer() }
    }

    private val checkForTimeout = EventHandler<ActionEvent> {

        timeoutAccural.value += TIMEOUT_CHECK_INTERVAL

        if (timeoutAccural >= TIMEOUTVAL) {

            if (timeline != null)
                timeline!!.stop()

            fire(TIMEOUT_EVENT)
        }
    }

    private fun startTimer() {
        timeline = Timeline(
                KeyFrame(
                        Duration.millis(TIMEOUT_CHECK_INTERVAL.toDouble()),
                        checkForTimeout
                )
        )
        timeline!!.cycleCount = INDEFINITE
        timeline!!.play()
    }

    private fun resetTimer() {
        timeoutAccural.value = 0L
    }
}