package no.ok.origo.dataplatform.csvtransformer

import no.ok.origo.csvlt.CsvTransformer
import no.ok.origo.dataplatform.steps.InputType
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtilsTest {

    @Test
    fun `test zero padding`() {
        val csvlt = """
            import "utils.jslt" as utils

            {
              *: utils:zero_pad(., 4)
            }
        """.trimIndent()

        val input = """
            a
            12
            12345
        """.trimIndent()

        val expected = """
            a
            0012
            12345

        """.trimIndent()

        val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
        val transformer = CsvTransformer(csvlt, format)

        val output = transformer.transform(input)

        assertEquals(expected, output)
    }

    // @Test
    fun `test range`() {
        val csvlt = """
            import "utils.jslt" as utils

            let root = .

            {
              for (utils:range(0, 5))
              let c = string(.)
              ${'$'}c : get-key(${'$'}root, ${'$'}c, 0)
            }
        """.trimIndent()

        val input = """
            1;2
            13;42
        """.trimIndent()

        val expected = """
            0;1;2;3;4
            0;13;42;0;0

        """.trimIndent()

        val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
        val transformer = CsvTransformer(csvlt, format)

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }

    @Test
    fun `test getcontentReader CSV`() {
        val stringInput = "blabla"
        val reader = getContentReader(stringInput.byteInputStream(), InputType.CSV)
        assert(reader.readText() == stringInput)
    }

    @Test
    fun `test getcontentReader GZIP`() {
        val stringInput = "blabla"
        val outputStream = ByteArrayOutputStream()
        val gzipper = GZIPOutputStream(outputStream)
        gzipper.write(stringInput.toByteArray())
        gzipper.close()

        val gzipInput = outputStream.toByteArray().inputStream()
        val reader = getContentReader(gzipInput, InputType.GZIP)
        assert(reader.readText() == stringInput)
    }
}
