package no.ok.origo.dataplatform.jsontransformer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.StringWriter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonTransformerTest {

    @Test
    fun `test transform`() {
        val jsltString = """
            let idparts = split(.id, "-")
            let xxx = [for (${'$'}idparts) "x" * size(.)]
            
            {
              "id" : join(${'$'}xxx, "-"),
              "type" : "Anonymized-View",
              * : .
            }
        """.trimIndent()

        val input = """
            {
            "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json",
            "id" : "w23q7ca1-8729-24923-922b-1c0517ddffjf1",
            "type" : "View"
            }""".replace("\\s".toRegex(), "")
        val expected = """
            {
            "id" : "xxxxxxxx-xxxx-xxxxx-xxxx-xxxxxxxxxxxxx",
            "type" : "Anonymized-View",
            "schema" : "http://schemas.schibsted.io/thing/pulse-simple.json#1.json"
            }
            """.replace("\\s".toRegex(), "")
        assertEquals(expected, JsonTransformer.transform(input, jsltString))
    }

    @Test
    fun `test writeCsv`() {
        val objectMapper = ObjectMapper()
        val jsonString = """[{"kake":"bløt","pris":20},{"kake":"ostekake","pris":20}]"""
        val format = CSVFormat.DEFAULT.withDelimiter(';')
            .withRecordSeparator("\n")
            .withQuote('"')
        val writer = StringWriter()
        val input: List<JsonNode> = objectMapper.readValue(jsonString)
        JsonTransformer.writeCsv(input, writer, format)
        writer.close()
        assertEquals("kake;pris\nbløt;20\nostekake;20\n", writer.toString())
    }
}
