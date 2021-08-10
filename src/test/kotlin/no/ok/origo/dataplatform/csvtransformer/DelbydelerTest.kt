package no.ok.origo.dataplatform.csvtransformer

import no.ok.origo.csvlt.CsvTransformer
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DelbydelerTest {

    val input = """
        Delbydelnummer,Delbydelsnavn
        11.0,Lodalen
        12.0,Grønland
        13.0,Enerhaugen
        14.0,Nedre Tøyen
        15.0,Kampen
        16.0,Vålerenga
        17.0,Helsfyr
        21.0,Grünerløkka vest
        22.0,Grünerløkka øst
    """.trimIndent()

    val expected = """
        delbydel_id,navn
        0101,Lodalen
        0102,Grønland
        0103,Enerhaugen
        0104,Nedre Tøyen
        0105,Kampen
        0106,Vålerenga
        0107,Helsfyr
        0201,Grünerløkka vest
        0202,Grünerløkka øst

    """.trimIndent()

    @Test
    fun `test enrich with subdistrict id by name`() {
        val csvlt = """
            import "delbydeler.jslt" as delbydeler

            let delbydel = delbydeler:fra_navn(.Delbydelsnavn)

            {
              "delbydel_id": ${'$'}delbydel.id,
              "navn": .Delbydelsnavn
            }
        """.trimIndent()

        val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
        val transformer = CsvTransformer(csvlt, format)

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }

    @Test
    fun `test enrich with subdistrict id by number (old)`() {
        val csvlt = """
            import "delbydeler.jslt" as delbydeler

            let delbydel = delbydeler:fra_gammel_id(number(.Delbydelnummer))

            {
              "delbydel_id": ${'$'}delbydel.id,
              "navn": .Delbydelsnavn
            }
        """.trimIndent()

        val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
        val transformer = CsvTransformer(csvlt, format)

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }
}
