package no.ok.origo.dataplatform.steps

enum class InputType(val extension: String) {
    GZIP("gz"), CSV("csv");

    companion object {
        fun fromExtension(extension: String): InputType? {
            return InputType.values().find {
                it.extension == extension
            }
        }
    }
}

class TooFewDatasets(totalDatasets: Int) : Exception("Too few datasets in input config: $totalDatasets")
class TooManyDatasets(totalDatasets: Int) : Exception("Too many datasets in input config: $totalDatasets")
class MissingStepConfig : Exception("Missing step config for current pipeline step")
