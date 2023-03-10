package by.derovi.botp2p.exchange

import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ResourceUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*


//fun main() z

object NetworkUtils {
    var credProvider: CredentialsProvider = BasicCredentialsProvider()
    val pool = mutableListOf<HttpHost>()
    val proxyDeque: Deque<HttpHost> = LinkedList()

    fun getProxy(): HttpHost = synchronized(proxyDeque) {
        val proxy = proxyDeque.removeFirst()
        proxyDeque.addLast(proxy)
        return proxy
    }

    init {
        val resourceLoader: ResourceLoader = DefaultResourceLoader()
        resourceLoader.getResource("classpath:proxies").inputStream.bufferedReader().forEachLine {
            val raw = it.split(":")
            val host = raw[0]
            val port = raw[1].toInt()
            val login = raw[2]
            val password = raw[3]
//            pool.add(HttpHost(host, port))
            println("$host $port $login $password")
            credProvider.setCredentials(AuthScope(host, port), UsernamePasswordCredentials(login, password))
            proxyDeque.add(HttpHost(host, port))
        }
    }

    fun getRequest2(urls: String) {
        val url = URL(urls)
        val connection = url.openConnection()
        BufferedReader(InputStreamReader(connection.getInputStream())).use { inp ->
            var line: String?
            while (inp.readLine().also { line = it } != null) {
                println(line)
            }
        }
    }

    fun createClient(proxy: HttpHost, disableUserAgent: Boolean = true) = with(HttpClients.custom()) {
        setDefaultRequestConfig(
            RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD).build()
        )
        if (disableUserAgent) {
            disableDefaultUserAgent()
        }
        disableAutomaticRetries()
        disableRedirectHandling()
        disableAuthCaching()
        disableConnectionState()
        disableCookieManagement()
        setProxy(proxy)
        setDefaultCredentialsProvider(credProvider)
        setDefaultHeaders(listOf())
        build()
    }

    fun getRequest(url: String): String {
        val proxy = getProxy()

        synchronized(proxy) {
            val client = createClient(proxy)

            val httpGet = HttpGet(url)
            val requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(5000)
                .setSocketTimeout(5000)
                .build()
            httpGet.config = requestConfig
            try {
                val result = client.execute(httpGet)
                return IOUtils.toString(result.entity.content, StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                println(proxy.hostName)
                ex.printStackTrace()
                throw ex
            } finally {
                httpGet.releaseConnection()
                client.close()
            }
        }
//        }
    }

    fun postRequest(url: String, payload: String, contentType: ContentType): String {
        val proxy = getProxy()

//        val client = clientPool.random()

        synchronized(proxy) {
            val client = createClient(proxy, false)

            val httpPost = HttpPost(url)
            httpPost.entity = StringEntity(payload, contentType)
            if (contentType == ContentType.APPLICATION_FORM_URLENCODED) {
                httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded")
            }
            val requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5000)
                .setConnectTimeout(5000)
                .setSocketTimeout(2000)
                .build()
            httpPost.config = requestConfig
            try {
                return IOUtils.toString(client.execute(httpPost).entity.content, StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                println(proxy.hostName)
                println(proxy.port)
                throw ex
            } finally {
                httpPost.releaseConnection()
                client.close()
            }
//        }
        }
    }
}