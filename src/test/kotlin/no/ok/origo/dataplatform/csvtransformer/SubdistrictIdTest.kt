package no.ok.origo.dataplatform.csvtransformer

import no.ok.origo.csvlt.CsvTransformer
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubdistrictIdTest {

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
        0301010101,Lodalen
        0301010102,Grønland
        0301010103,Enerhaugen
        0301010104,Nedre Tøyen
        0301010105,Kampen
        0301010106,Vålerenga
        0301010107,Helsfyr
        0301020201,Grünerløkka vest
        0301020202,Grünerløkka øst

    """.trimIndent()

    @Test
    fun `test enrich with subdistrict id by name`() {
        val csvlt = """
            import "subdistrict-id.jslt" as delbydel_id

            {
              "delbydel_id": delbydel_id:get_by_name(.Delbydelsnavn),
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
            import "subdistrict-id.jslt" as delbydel_id

            {
              "delbydel_id": delbydel_id:get_by_number_old(.Delbydelnummer),
              "navn": .Delbydelsnavn
            }
        """.trimIndent()

        val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
        val transformer = CsvTransformer(csvlt, format)

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }
}
