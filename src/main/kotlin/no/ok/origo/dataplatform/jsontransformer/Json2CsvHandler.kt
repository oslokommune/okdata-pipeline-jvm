package no.ok.origo.dataplatform.jsontransformer

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.ok.origo.dataplatform.commons.lambda.DataplatformLoggingHandler
import no.ok.origo.dataplatform.commons.pipeline.config.Config
import no.ok.origo.dataplatform.commons.pipeline.config.StepData
import org.apache.commons.csv.CSVFormat
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.StringWriter

class Json2CsvHandler(private val s3: S3Client = S3Client.create()) : DataplatformLoggingHandler() {
    val s3Bucket = System.getenv("BUCKET_NAME") ?: System.getProperty("BUCKET_NAME")
    val mapper = jacksonObjectMapper()

    override fun handleRequestWithLogging(input: InputStream, output: OutputStream, context: Context) {
        val config = mapper.readValue<Config>(input)
        val result = handleRequest(config)
        mapper.writeValue(output, result)
    }

    fun handleRequest(config: Config): StepData {

        logAdd("execution_name" to config.executionName, "task" to config.task)

        val stepData = config.payload.stepData
        if (stepData.s3InputPrefixes == null) {
            throw Exception("Json2CSV is only supported for file-based (S3) datasets")
        }

        val (_, s3InputPrefix) = stepData.s3InputPrefixes!!.iterator().next()

        logAdd("s3_input_prefix" to s3InputPrefix)
        val s3Objects = listS3ObjectsOnPrefix(s3InputPrefix)
        logAdd("s3_input_keys" to s3Objects.map { it.key() })
        val outputPrefix = config.getIntermediatePrefix()
        logAdd("s3_output_prefix" to outputPrefix)
        s3Objects.forEach { s3Object ->

            val inputS3Key = s3Object.key()
            val inputJsonString = readFromS3(inputS3Key)

            val csvWriter = StringWriter()
            JsonTransformer.writeCsv(inputToJsonList(inputJsonString), csvWriter, csvFormat(config.json2CsvConfig()))

            val (filename, _) = s3Object.partitionExtension()
            val outputS3Key = "$outputPrefix$filename.csv"
            writeToS3(outputS3Key, csvWriter.toString())
        }

        return StepData(
            inputEvents = null,
            s3InputPrefixes = mapOf(config.payload.outputDataset.id to outputPrefix),
            status = "Success",
            errors = emptyList()
        )
    }

    private fun inputToJsonList(inputJsonString: String): List<JsonNode> {
        return mapper.readValue(inputJsonString)
    }

    private fun csvFormat(config: Json2CsvConfig): CSVFormat {
        return CSVFormat.DEFAULT
            .withDelimiter(config.delimiter)
            .withRecordSeparator(config.record_separator)
            .withQuote(config.quote)
            .withSkipHeaderRecord(! config.header_row)
    }

    fun readFromS3(s3Key: String): String {
        return s3.getObject { r -> r.bucket(s3Bucket).key(s3Key) }.use { inputStream ->
            InputStreamReader(inputStream, Charsets.UTF_8).buffered().use { reader ->
                reader.readText()
            }
        }
    }

    fun writeToS3(s3Key: String, outputJson: String) {
        s3.putObject({ r -> r.bucket(s3Bucket).key(s3Key) }, RequestBody.fromString(outputJson))
    }

    fun listS3ObjectsOnPrefix(prefix: String): List<software.amazon.awssdk.services.s3.model.S3Object> {
        val result = s3.listObjectsV2 {
            it.bucket(s3Bucket)
                .prefix(prefix)
                .build()
        }
        return result.contents().toList()
    }
}

data class Json2CsvConfig(
    val header_row: Boolean = true,
    val delimiter: Char = ';',
    val record_separator: Char = '\n',
    val quote: Char = '"'
)

fun Config.json2CsvConfig(): Json2CsvConfig {
    return jacksonObjectMapper().convertValue(this.getTaskConfig()) ?: Json2CsvConfig()
}
