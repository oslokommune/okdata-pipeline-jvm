package no.ok.origo.dataplatform.csvtransformer

import com.adobe.testing.s3mock.junit5.S3MockExtension
import no.ok.origo.dataplatform.TestContext
import no.ok.origo.dataplatform.jsontransformer.TestUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandlerTest : S3MockExtension() {

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

    private lateinit var s3Client: S3Client
    val bucket = System.getenv("BUCKET_NAME")

    override fun start() {
        super.start()
        TestUtils.fixUntrustCertificate()
    }

    @BeforeAll
    fun init() {
        start()
        s3Client = createS3ClientV2()

        val s3Bucket = CreateBucketRequest.builder()
            .bucket(bucket)
            .build()

        s3Client.createBucket(s3Bucket)

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key("raw/green/boligpriser/version=1/edition=20200120T133700/test.csv")
                .build(),
            RequestBody.fromString("a;b;c\n1;2;3\n4;5;6")
        )
    }

    @Test
    fun `integration test Handler`() {
        val handler = Handler(s3Client)
        val output = ByteArrayOutputStream()

        val ctx = TestContext("aws-request-id-1234")
        handler.handleRequest(json_input_string.byteInputStream(), output, ctx)
        val outputKey = "intermediate/green/boligpriser/version=1/edition=20200120T133701/transform_csv/test.csv"
        val response = s3Client.getObject {
            it.bucket(bucket).key(outputKey).build()
        }.bufferedReader().readText()
        assert(response == "d;e;f\n2;1;3\n5;4;6\n")
    }
}
