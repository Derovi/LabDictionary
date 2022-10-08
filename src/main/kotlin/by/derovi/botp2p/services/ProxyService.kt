package by.derovi.botp2p.services

import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import java.io.File
import javax.annotation.PostConstruct

@Service
class ProxyService {
//    var credProvider: CredentialsProvider = BasicCredentialsProvider()
//    val pool = mutableListOf<HttpHost>()
//
//    @PostConstruct
//    fun loadProxies() {
//        ResourceUtils.getFile("classpath:proxies").forEachLine {
//            println(it)
//            val raw = it.split(":")
//            val host = raw[0]
//            val port = raw[1].toInt()
//            val login = raw[2]
//            val password = raw[3]
//            pool.add(HttpHost(host, port))
//            credProvider.setCredentials(AuthScope(host, port), UsernamePasswordCredentials(login, password))
//        }
//    }
//
//    fun random() = pool.random()
}