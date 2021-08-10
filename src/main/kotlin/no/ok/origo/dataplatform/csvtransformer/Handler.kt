package no.ok.origo.dataplatform.csvtransformer

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.ok.origo.csvlt.CsvTransformer
import no.ok.origo.dataplatform.commons.lambda.DataplatformLoggingHandler
import no.ok.origo.dataplatform.commons.pipeline.config.Config
import no.ok.origo.dataplatform.steps.InputType
import no.ok.origo.dataplatform.steps.TooFewDatasets
import no.ok.origo.dataplatform.steps.TooManyDatasets
import no.ok.origo.dataplatform.steps.partitionExtension
import org.apache.commons.csv.CSVFormat
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.util.zip.GZIPInputStream

class Handler : DataplatformLoggingHandler() {
    val bucket = System.getenv("BUCKET_NAME") ?: System.getProperty("BUCKET_NAME")
    val s3: S3Client = S3Client.create()
    private val om = jacksonObjectMapper()

    override fun handleRequestWithLogging(input: InputStream, output: OutputStream, context: Context) {
        val config = om.readValue<Config>(input)
        val stepConfig = om.convertValue<StepConfig>(config.getTaskConfig())

        val transformer = CsvTransformer(stepConfig.csvlt, stepConfig.csvFormat)
        val outputPrefix = config.getIntermediatePrefix()

        val countInputs = config.payload.stepData.s3InputPrefixes!!.size
        when {
            countInputs < 1 -> throw TooFewDatasets(countInputs)
            countInputs > 1 -> throw TooManyDatasets(countInputs)
        }

        val (_, s3InputPrefix) = config.payload.stepData.s3InputPrefixes!!.iterator().next()

        logAdd("s3_input_prefixes" to s3InputPrefix)
        val objects = listObjectsOnPrefix(s3InputPrefix)

        objects.forEach { s3Object ->
            val inputKey = s3Object.key()
            val (filename, extension) = s3Object.partitionExtension()
            val inputType = InputType.fromExtension(extension) ?: throw Exception("Illegal input data type")
            val outputKey = "$outputPrefix$filename.${InputType.CSV.extension}"
            logAdd("s3_input_key" to inputKey)
            val inputStream = s3.getObject {
                it.bucket(bucket).key(inputKey).build()
            }

            logAdd("s3_output_key" to outputKey)
            val reader = getContentReader(inputStream, inputType)
            transformAndUploadFile(reader, outputKey, transformer)
        }

        config.payload.stepData.s3InputPrefixes = mapOf(config.payload.outputDataset.id to outputPrefix)
        om.writeValue(output, config.payload.stepData)
    }

    fun transformAndUploadFile(
        reader: Reader,
        outputKey: String,
        transformer: CsvTransformer
    ): PutObjectResponse? {

        val tempFile = createTempFile()
        tempFile.deleteOnExit()
        tempFile.bufferedWriter().use {
            transformer.transform(reader, it)
        }
        return s3.putObject(PutObjectRequest.builder().bucket(bucket).key(outputKey).build(), tempFile.toPath())
    }

    fun listObjectsOnPrefix(prefix: String): List<S3Object> {
        val result = s3.listObjectsV2 {
            it.bucket(bucket)
                .prefix(prefix)
                .build()
        }
        return result.contents().toList()
    }
}

fun getContentReader(inputStream: InputStream, type: InputType): Reader {
    return when (type) {
        InputType.CSV -> inputStream.bufferedReader()
        InputType.GZIP -> GZIPInputStream(inputStream).bufferedReader()
    }
}

data class StepConfig(
    val csvlt: String,
    val header_row: Boolean = true,
    val skip_header_record: Boolean? = false,
    val delimiter: Char = ';',
    val record_separator: Char = '\n',
    val quote: Char = '"'

) {
    var csvFormat: CSVFormat = CSVFormat
        .newFormat(delimiter)
        .withRecordSeparator(record_separator)
        .withQuote(quote)

    init {
        if (header_row) {
            csvFormat = csvFormat.withHeader()
        }
        if (skip_header_record == true)
            csvFormat = csvFormat.withSkipHeaderRecord()
    }
}
