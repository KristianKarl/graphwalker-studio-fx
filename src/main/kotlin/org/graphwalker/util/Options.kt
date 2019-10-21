package org.graphwalker.util

import com.beust.jcommander.Parameter
import java.util.*

class Options {

    @Parameter(names = { "--help", "-h" }, description = "Prints help text")
    var help = false

    @Parameter(names = { "--version", "-v" }, description = "Prints the version of graphwalker")
    var version = false

    @Parameter(names = { "--debug", "-d" }, description = "Sets the log level: OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL. Default is OFF")
    var debug = "OFF"

    @Parameter(names = { "--model", "-m" }, required = false, variableArity = true, description = "The model, as a file")
    var modelFiles: List<String> = ArrayList()
}
