package no.ok.origo.dataplatform.jsontransformer

import com.adobe.testing.s3mock.junit5.S3MockExtension
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.ok.origo.dataplatform.TestContext
import no.ok.origo.dataplatform.commons.pipeline.config.StepData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Json2JsonHandlerTest : S3MockExtension() {

    private val om = jacksonObjectMapper()
    private val testBucketName = "testbucket"
    private val ctx = TestContext("some-request-id")
    private lateinit var s3Client: S3Client

    override fun start() {
        super.start()
        TestUtils.fixUntrustCertificate()
    }

    private fun createBucket(bucketName: String) {
        val s3Bucket = CreateBucketRequest.builder()
            .bucket(bucketName)
            .build()

        s3Client.createBucket(s3Bucket)
    }

    @BeforeAll
    fun init() {
        start()
        s3Client = createS3ClientV2()
        createBucket(testBucketName)
    }

    private val inputJson = om.readTree(
        """
            {
              "schema": "http://schemas.io/thing/simple.json",
              "id": "w23q7ca1-8729-24923-922b-1c0517ddffjf1",
              "type": "View"
            }
        """.trimIndent()
    )

    private val transformedJson = om.readTree(
        """
            {
              "schema": "http://schemas.io/thing/simple.json",
              "id": "xxxxxxxx-xxxx-xxxxx-xxxx-xxxxxxxxxxxxx",
              "type": "Anonymized-View"
            }
        """.trimIndent()
    )

    @Test
    fun `test write to and read from s3`() {

        val s3Object = S3Object(testBucketName, "test_key.json")

        val handler = Json2JsonHandler(s3Client)

        handler.writeToS3(s3Object, inputJson)

        assertEquals(inputJson, handler.readFromS3(s3Object))
    }

    @Test
    fun `test handle s3 event`() {

        val lambdaEvent = TestUtils.readTestResource("json2json/s3-config.json")

        val inputKey = "raw/yellow/input-dataset-id/version=1/edition=20190131T000000/data.json"
        val outputKey = "intermediate/yellow/output-dataset-id/version=1/edition=20190131T000000/json2json/data.json"

        val handler = Json2JsonHandler(s3Client)

        handler.writeToS3(S3Object(testBucketName, inputKey), inputJson)

        val output = ByteArrayOutputStream()
        handler.handleRequest(lambdaEvent.byteInputStream(), output, ctx)
        val result = om.readValue<StepData>(output.toByteArray())

        val expectedOutputPrefix = "intermediate/yellow/output-dataset-id/version=1/edition=20190131T000000/json2json/"
        assertEquals(expectedOutputPrefix, result.s3InputPrefixes!!["output-dataset-id"])

        assertEquals(transformedJson, handler.readFromS3(S3Object(testBucketName, outputKey)))
    }

    @Test
    fun `test handle inline event`() {
        val lambdaEvent = TestUtils.readTestResource("json2json/event-config.json")

        val handler = Json2JsonHandler(s3Client)

        val output = ByteArrayOutputStream()
        handler.handleRequestWithLogging(lambdaEvent.byteInputStream(), output, ctx)
        val result = om.readValue<StepData>(output.toByteArray())

        assertEquals(transformedJson, result.inputEvents!![0])
    }
}
