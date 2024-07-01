package no.ok.origo.dataplatform.jsontransformer

import com.adobe.testing.s3mock.junit5.S3MockExtension
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.ok.origo.dataplatform.TestContext
import no.ok.origo.dataplatform.commons.pipeline.config.Config
import no.ok.origo.dataplatform.commons.pipeline.config.StepData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Json2CsvHandlerTest : S3MockExtension() {

    private val ctx = TestContext("some-request-id")

    private val om = jacksonObjectMapper()
    private lateinit var s3Client: S3Client

    override fun start() {
        super.start()
        TestUtils.fixUntrustCertificate()
    }

    private fun createBucket() {
        val s3Bucket = CreateBucketRequest.builder()
            .bucket(System.getenv("BUCKET_NAME"))
            .build()

        s3Client.createBucket(s3Bucket)
    }

    @BeforeAll
    fun init() {
        start()
        s3Client = createS3ClientV2()
        createBucket()
    }

    @Test
    fun `test write to and read from s3`() {

        val s3Key = "inputprefix/test_input.json"

        val handler = Json2CsvHandler(s3Client)

        val jsonString = TestUtils.readTestResource("json2csv/json2CsvInput.json")
        handler.writeToS3(s3Key, jsonString)

        Assertions.assertEquals(jsonString, handler.readFromS3(s3Key))
    }

    @Test
    fun `test handle s3 event`() {

        val lambdaEvent = TestUtils.readTestResource("json2csv/json2CsvConfig.json")
        val config = om.readValue<Config>(lambdaEvent)

        val handler = Json2CsvHandler(s3Client)

        val inputJson = TestUtils.readTestResource("json2csv/json2CsvInput.json")
        handler.writeToS3("raw/yellow/input-dataset-id/version=1/edition=20190131T000000/test_data.json", inputJson)

        val output = ByteArrayOutputStream()
        handler.handleRequestWithLogging(lambdaEvent.byteInputStream(), output, ctx)
        val result = om.readValue<StepData>(output.toByteArray())

        val expectedInputPrefixes = mapOf(config.payload.outputDataset.id to config.getIntermediatePrefix())
        Assertions.assertEquals(StepData(null, expectedInputPrefixes, "Success", emptyList()), result)

        val expectedCsv = TestUtils.readTestResource("json2csv/json2CsvOutput.csv")
        Assertions.assertEquals(expectedCsv, handler.readFromS3("intermediate/yellow/output-dataset-id/version=1/edition=20190131T000000/json2csv/test_data.csv"))
    }
}
