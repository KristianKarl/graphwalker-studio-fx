package org.graphwalker.util

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object LoggerUtil {

    enum class Level {
        OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
    }

    fun setLogLevel(level: Level) {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = ch.qos.logback.classic.Level.valueOf(level.name)
    }
}
