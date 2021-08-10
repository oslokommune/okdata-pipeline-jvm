package no.ok.origo.dataplatform.jsontransformer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.jvm.Throws

object TestUtils {

    val mapper = jacksonObjectMapper()

    /**
     *   Workaround for java.security.cert.CertificateException: No name matching localhost found
     *   Occurs when using S3MockExtension
     *   https://stackoverflow.com/questions/3093112/certificateexception-no-name-matching-ssl-someurl-de-found/33846993#33846993?newreg=4d421399f6894c79bb802376caac4b18
     **/
    @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
    fun fixUntrustCertificate() {

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? {
                return null
            }

            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        })

        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        val allHostsValid = HostnameVerifier { _, _ -> true }

        // set the  allTrusting verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
    }

    inline fun <reified T : Any> readJson(filename: String): T {
        val jsonString = readTestResource(filename)
        return mapper.readValue(jsonString, T::class.java)
    }

    fun readTestResource(filename: String): String {
        return this::class.java.classLoader.getResource(filename).readText()
    }
}
