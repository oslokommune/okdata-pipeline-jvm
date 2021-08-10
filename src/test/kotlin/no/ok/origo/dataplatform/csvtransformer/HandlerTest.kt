package no.ok.origo.dataplatform.csvtransformer

import io.findify.s3mock.S3Mock
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.ok.origo.dataplatform.commons.lambda.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandlerTest {

    val json_input_string = """{
  "execution_name": "boligpriser-UUID",
  "task": "transform_csv",
  "payload": {
    "pipeline": {
      "id": "boligpriser",
      "task_config": {
        "write_cleaned": {
          "output_stage": "cleaned"
        },
        "transform_csv": {
          "delimiter": ";",
          "header_row": true,
          "csvlt": "{ \"d\": .b, \"e\": .a, \"f\": .c }"
        }
      }
    },
    "output_dataset": {
      "id": "boligpriser",
      "version": "1",
      "edition": "20200120T133700",
      "s3_prefix": "%stage%/green/boligpriser/version=1/edition=20200120T133701/"
    },
    "step_data": {
      "input_events": null,
      "s3_input_prefixes": {
        "boligpriser": "raw/green/boligpriser/version=1/edition=20200120T133700/"
      },
      "status": "OK",
      "errors": []
    }
  }
}"""
    lateinit var api: S3Mock
    val bucket = "test-bucket"

    @AfterEach
    fun afterEach() {
        api.shutdown()
        unmockkStatic(S3Client::class)
    }

    @BeforeEach
    fun beforeEach() {
        val availablePort = getAvailablePort()
        System.setProperty("BUCKET_NAME", bucket)
        api = S3Mock.Builder().withPort(availablePort).withInMemoryBackend().build()
        api.start()

        val localS3Client = S3Client.builder()
            .endpointOverride(URI.create("http://localhost:$availablePort"))
            .region(Region.EU_WEST_1)
            .build()
        localS3Client.createBucket { it.bucket(bucket).build() }

        localS3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key("raw/green/boligpriser/version=1/edition=20200120T133700/test.csv")
                .build(),
            RequestBody.fromString("a;b;c\n1;2;3\n4;5;6")
        )
        mockkStatic(S3Client::class)
        every { S3Client.create() } returns localS3Client
    }

    @Test
    fun `integration test Handler`() {
        val handler = Handler()
        val output = ByteArrayOutputStream()

        handler.handleRequest(json_input_string.byteInputStream(), output, TestContext())
        val outputKey = "intermediate/green/boligpriser/version=1/edition=20200120T133701/transform_csv/test.csv"
        val response = handler.s3.getObject {
            it.bucket(bucket).key(outputKey).build()
        }.bufferedReader().readText()
        assert(response == "d;e;f\n2;1;3\n5;4;6\n")
    }

    fun getAvailablePort(): Int {
        return ServerSocket(0).localPort
    }
}
