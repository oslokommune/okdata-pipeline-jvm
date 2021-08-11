package no.ok.origo.dataplatform

import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.CognitoIdentity
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import kotlin.random.Random

class TestContext(val requestId: String) : Context {

    override fun getAwsRequestId(): String {
        return requestId
    }

    override fun getLogStreamName(): String {
        TODO("not implemented")
    }

    override fun getClientContext(): ClientContext {
        TODO("not implemented")
    }

    override fun getFunctionName() = "function-name-1234"

    override fun getRemainingTimeInMillis() = Random.nextInt(from = 200, until = 5000)

    override fun getLogger(): LambdaLogger {
        TODO("not implemented")
    }

    override fun getInvokedFunctionArn() = "arn:for:the:invoked:lambda:$functionName"

    override fun getMemoryLimitInMB() = 512

    override fun getLogGroupName(): String {
        TODO("not implemented")
    }

    override fun getFunctionVersion(): String {
        TODO("not implemented")
    }

    override fun getIdentity(): CognitoIdentity {
        TODO("not implemented")
    }
}
