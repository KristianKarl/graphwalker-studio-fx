package org.graphwalker.views

import org.graphwalker.core.generator.PathGenerator
import org.graphwalker.core.machine.ExecutionContext
import org.graphwalker.core.model.Model
import javax.script.SimpleBindings

class ExecutionContextFX : ExecutionContext {

    constructor() : super() {
        scriptEngine.put("global", bindings)
    }

    constructor(model: Model, generator: PathGenerator<*>) : super(model, generator) {
        scriptEngine.put("global", bindings)
    }

    constructor(model: Model.RuntimeModel, generator: PathGenerator<*>) : super(model, generator) {
        scriptEngine.put("global", bindings)
    }

    companion object {

        private val bindings = SimpleBindings()
    }
}
