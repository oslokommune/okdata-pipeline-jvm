package no.ok.origo.dataplatform.jsontransformer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.Parser
import org.apache.commons.csv.CSVFormat
import java.io.IOException
import java.io.Writer
import java.util.LinkedHashSet

object JsonTransformer {

    private val om: ObjectMapper = jacksonObjectMapper()

    fun transform(inputJson: String, jsltString: String): String {
        val jslt = Parser.compileString(jsltString)
        return transform(inputJson, jslt)
    }

    fun transform(inputJson: String, jslt: Expression): String {
        val json = om.readTree(inputJson)
        val transformedJson = jslt.apply(json)
        return om.writeValueAsString(transformedJson)
    }

    @Throws(IOException::class)
    fun writeCsv(rows: Collection<JsonNode>, output: Writer, format: CSVFormat) {
        val fieldNames = getFieldNames(rows)
        val csvPrinter = format
            .withHeader(*fieldNames.toTypedArray())
            .print(output)

        for (row in rows) {
            val values: List<String> = fieldNames
                .map {
                    row.findValue(it).asText()
                }
            csvPrinter.printRecord(values)
        }
    }

    private fun getFieldNames(rows: Collection<JsonNode>): Collection<String> {
        val fieldNames = LinkedHashSet<String>()
        for (row in rows) {
            row.fieldNames().forEachRemaining { fieldNames.add(it) }
        }
        return fieldNames
    }
}
