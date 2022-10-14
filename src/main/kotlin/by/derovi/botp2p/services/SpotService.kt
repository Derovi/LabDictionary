package by.derovi.botp2p.services

import by.derovi.botp2p.exchange.NetworkUtils
import by.derovi.botp2p.exchange.Token
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

@Service
class SpotService {
    final var prices = mapOf<Token, Double>()
        private set

    val tokenToSymbol = mapOf(
        Token.BTC to "BTCUSDT",
        Token.ETH to "ETHUSDT",
        Token.BCH to "BCHUSDT",
        Token.BNB to "BNBUSDT",
        Token.EOS to "EOSUSDT",
        Token.XRP to "XRPUSDT",
        Token.LTC to "LTCUSDT",
    )

    fun price(token: Token): Double =
        prices[token] ?: throw java.lang.IllegalArgumentException("No spot price found for $token")

    val defaultPrices = listOf(Token.USDT, Token.HUSD, Token.BUSD, Token.USDC).associateWith { 1.0 }

    @PostConstruct
    fun checkMapping() {

        val withoutMapping = Token.values().toSet() subtract defaultPrices.keys subtract tokenToSymbol.keys

        if (withoutMapping.isNotEmpty()) {
            println("WARNING! These tokens has no mapping in SpotService " +
                    withoutMapping.joinToString(", " )
            )
        }
    }

    @Scheduled(fixedRate = 10000)
    fun updateInfo() {
        thread {
            try {
                val newPrices = mutableMapOf<Token, Double>()
                newPrices.putAll(defaultPrices)
                for (token in tokenToSymbol.keys) {
                    val response = NetworkUtils.getRequest(
                        "https://api.binance.com/api/v3/ticker/price?symbol=" + tokenToSymbol[token]
                    )
                    val matcher = "price\\\":\\\"(.*)\\\"".toRegex()
                    matcher.find(response)?.groups?.get(1)?.value?.toDoubleOrNull()?.also {
                        newPrices[token] = it
                    }
                }
                prices = newPrices
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}