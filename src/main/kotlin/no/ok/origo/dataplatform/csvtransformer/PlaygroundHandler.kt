package no.ok.origo.dataplatform.csvtransformer

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.ok.origo.csvlt.CsvTransformer
import no.ok.origo.dataplatform.commons.lambda.DataplatformLoggingHandler
import org.apache.commons.csv.CSVFormat
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader

class PlaygroundHandler : DataplatformLoggingHandler() {
    override fun handleRequestWithLogging(input: InputStream, output: OutputStream, context: Context) {
        val om = jacksonObjectMapper()

        val request = om.readTree(input)
        context.logger.log("Request: " + om.writeValueAsString(request))

        val body = om.readTree(StringReader(request.get("body").textValue()))

        val format = CSVFormat.DEFAULT
            .withDelimiter(';')
            .withHeader()

        val transformer = CsvTransformer(body.get("csvlt").textValue(), format)
        val result = transformer.transform(body.get("input").textValue())

        val headers = om.createObjectNode()
        headers.put("Access-Control-Allow-Origin", "*")
        headers.put("Access-Control-Allow-Credentials", true)

        val response = om.createObjectNode()
        response.put("statusCode", 200)
        response.replace("headers", headers)
        response.put("body", result)

        om.writeValue(output, response)
    }
}

data class PlaygroundRequest(
    var input: String? = "",
    var csvlt: String? = ""
)
