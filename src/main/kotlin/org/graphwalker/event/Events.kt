package org.graphwalker.event

import org.graphwalker.model.EdgeFX
import org.graphwalker.model.VertexFX
import org.graphwalker.views.ModelEditorView
import tornadofx.*
import java.io.File

class LoadModelsFromFileEvent(val modelFile: File) : FXEvent()
class LoadedModelsFromFileEvent : FXEvent()

class NevModelEditorEvent(val modelEditorView: ModelEditorView) : FXEvent()

class RunModelsEvent : FXEvent()
class RunModelsDoneEvent : FXEvent()
class RunModelsStopEvent : FXEvent()

class ProgressEvent(val completed: Double) : FXEvent()
class SelectModelEditor(val modelEditorView: ModelEditorView) : FXEvent()

class ModelsAreChangedEvent : FXEvent()
class ModelsAreSavedEvent : FXEvent()
class ClearAllModelsEvent : FXEvent()

class OpenPropertiesView : FXEvent()
class DisableElementProperties : FXEvent()
class EnableElementProperties : FXEvent()

class VertexSelectedEvent(val vertex: VertexFX) : FXEvent()
class EdgeSelectedEvent(val edge: EdgeFX) : FXEvent()