package org.graphwalker.exceptions

class UnsupportedFileFormat(path: String) : Throwable("File format for file: $path, is not supported")
