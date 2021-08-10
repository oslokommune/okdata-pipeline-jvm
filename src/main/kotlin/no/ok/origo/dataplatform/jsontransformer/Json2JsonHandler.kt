package no.ok.origo.dataplatform.jsontransformer

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.schibsted.spt.data.jslt.Expression
import com.schibsted.spt.data.jslt.Parser
import no.ok.origo.dataplatform.commons.lambda.DataplatformLoggingHandler
import no.ok.origo.dataplatform.commons.pipeline.config.Config
import no.ok.origo.dataplatform.commons.pipeline.config.StepData
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream
import java.io.OutputStream

class Json2JsonHandler(private val s3: S3Client = S3Client.create()) : DataplatformLoggingHandler() {

    val bucket = System.getenv("BUCKET_NAME") ?: System.getProperty("BUCKET_NAME")
    val mapper = jacksonObjectMapper()

    override fun handleRequestWithLogging(input: InputStream, output: OutputStream, context: Context) {
        val config = mapper.readValue<Config>(input)
        val result = handleRequest(config)
        mapper.writeValue(output, result)
    }

    fun handleRequest(config: Config): StepData {
        logAdd("execution_name" to config.executionName, "task" to config.task)

        val taskConfig = config.getTaskConfig()
        val jslt = Parser.compileString(taskConfig.get("jslt").textValue())

        val stepData = config.payload.stepData

        return if (stepData.s3InputPrefixes != null && stepData.s3InputPrefixes!!.isNotEmpty()) {
            handleS3Event(config, jslt)
        } else if (stepData.inputEvents != null && stepData.inputEvents!!.isNotEmpty()) {
            stepData.inputEvents = stepData.inputEvents!!.map(jslt::apply)
            stepData
        } else {
            throw RuntimeException("No data to transform")
        }
    }

    private fun handleS3Event(config: Config, jslt: Expression): StepData {
        val outputDataset = config.payload.outputDataset
        val stepData = config.payload.stepData

        val numInputs = stepData.s3InputPrefixes!!.size
        if (numInputs > 1) {
            throw TooManyDatasets(numInputs)
        }

        val (_, s3InputPrefix) = stepData.s3InputPrefixes!!.iterator().next()
        logAdd("s3_input_prefix" to s3InputPrefix)
        val s3Objects = listObjectsOnPrefix(s3InputPrefix)
        logAdd("s3_input_keys" to s3Objects.map { it.key() })
        val outputPrefix = config.getIntermediatePrefix()
        logAdd("s3_output_prefix" to outputPrefix)
        s3Objects.forEach { s3Object ->
            val inputKey = s3Object.key()
            val (filename, extension) = s3Object.partitionExtension()
            val outputKey = "$outputPrefix$filename.$extension"

            val inputJson = readFromS3(S3Object(bucket, inputKey))

            val outputJson = jslt.apply(inputJson)
            writeToS3(S3Object(bucket, outputKey), outputJson)
        }
        stepData.s3InputPrefixes = mapOf(outputDataset.id to outputPrefix)
        return stepData
    }

    fun listObjectsOnPrefix(prefix: String): List<software.amazon.awssdk.services.s3.model.S3Object> {
        val result = s3.listObjectsV2 {
            it.bucket(bucket)
                .prefix(prefix)
                .build()
        }
        return result.contents().toList()
    }

    fun readFromS3(s3Object: S3Object): JsonNode {
        return s3.getObject { r -> r.bucket(s3Object.bucket).key(s3Object.key) }.use { inputStream ->
            mapper.readTree(inputStream)
        }
    }

    fun writeToS3(s3Object: S3Object, json: JsonNode) {
        val jsonString = mapper.writeValueAsString(json)
        s3.putObject({ r -> r.bucket(s3Object.bucket).key(s3Object.key) }, RequestBody.fromString(jsonString))
    }
}

class TooManyDatasets(totalDatasets: Int) : Exception("Too many datasets in input config: $totalDatasets")
