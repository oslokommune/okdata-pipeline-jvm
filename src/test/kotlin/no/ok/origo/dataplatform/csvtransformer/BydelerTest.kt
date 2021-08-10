package no.ok.origo.dataplatform.csvtransformer

import no.ok.origo.csvlt.CsvTransformer
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BydelerTest {

    val csvlt = """
        import "bydeler.jslt" as bydeler

        let bydel = bydeler:fra_navn(.navn)

        {
          "id": ${'$'}bydel.id,
          "navn": ${'$'}bydel.navn
        }
    """.trimIndent()

    val format = CSVFormat.DEFAULT.withRecordSeparator('\n').withHeader()
    val transformer = CsvTransformer(csvlt, format)

    @Test
    fun `test enrich with district id by name`() {
        val input = """
            navn
            Bydel Gamle Oslo
            Bydel Grünerløkka
            Bydel St Hanshaugen
            Bydel Østensjø
            Sentrum
        """.trimIndent()

        val expected = """
            id,navn
            01,Bydel Gamle Oslo
            02,Bydel Grünerløkka
            04,Bydel St. Hanshaugen
            13,Bydel Østensjø
            16,Sentrum
    
        """.trimIndent()

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }

    @Test
    fun `getting district by name should handle missing 'Bydel' prefix`() {
        val input = """
        navn
        Gamle Oslo
        Bydel Grünerløkka
        """.trimIndent()

        val expected = """
        id,navn
        01,Bydel Gamle Oslo
        02,Bydel Grünerløkka

        """.trimIndent()

        val output = transformer.transform(input)
        assertEquals(expected, output)
    }
}
