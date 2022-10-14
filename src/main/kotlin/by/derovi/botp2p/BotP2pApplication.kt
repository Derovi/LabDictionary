package by.derovi.botp2p

import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.exchanges.*
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling


@SpringBootApplication(proxyBeanMethods = true)
@EnableScheduling
class BotP2pApplication : CommandLineRunner {

    @Bean
    fun bundleSearchBean() = BundleSearch(
        arrayOf(Huobi, Binance, OKX, Bybit, Kucoin, Bitzlato)
//        arrayOf(Kucoin)
//        arrayOf(Binance)
    )

    override fun run(vararg args: String?) {
        Thread.currentThread().join()
    }
}

fun main(args: Array<String>) {
//    println(NetworkUtils.getRequest("https://www.okx.com/v3/c2c/tradingOrders/books?t=1665098356004&quoteCurrency=rub&baseCurrency=usdc&side=buy&paymentMethod=bank&userType=all&showTrade=false&showFollow=false&showAlreadyTraded=false&isAbleFilter=false"))
    runApplication<BotP2pApplication>(*args)
}
