package no.ok.origo.dataplatform.csvtransformer
import java.lang.Exception

data class CSVLTEvent(
    val input: Map<String, String>,
    val output: String,
    val csvlt: String,
    val header_row: Boolean = true,
    val delimiter: Char = ';',
    val record_separator: Char = '\n',
    val quote: Char = '"'
)

data class MutableCSVLTEvent(
    var input: Map<String, String>? = null,
    var output: String? = null,
    var config: MutableConfig? = null

) {
    fun toCSVLTEvent(): CSVLTEvent {
        if (config == null) {
            throw Exception("Config Missing")
        }
        return CSVLTEvent(
            input = input!!,
            output = output!!,
            csvlt = config!!.csvlt ?: throw Exception("CSVLT Missing"),
            header_row = config?.header_row ?: true,
            delimiter = config?.delimiter ?: ';',
            record_separator = config?.record_separator ?: '\n',
            quote = config?.quote ?: '"'
        )
    }
}

data class MutableConfig(
    var csvlt: String? = null,
    var header_row: Boolean? = true,
    var delimiter: Char? = ';',
    var record_separator: Char? = '\n',
    var quote: Char? = '"'
)
