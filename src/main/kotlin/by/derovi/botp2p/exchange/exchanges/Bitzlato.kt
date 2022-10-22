package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.exchange.Currency
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.*

//class TokenGenerator : () -> String {
//    private val jsonWebKey = """
//        {
//            "kty":"EC","alg":"ES256","crv":"P-256",
//            "x":"tLOYKCx9vqNHf7Rki_XgUy3BIaATYlLPq0i_x6uNqkM",
//            "y":"HbyYeECqJZp1INYaIsgVdoZb52104RHDNrG54gTzZjM",
//            "d":"cMdRLYGvIh0kmN4JE7HKz9G4miDL2yN5PiA8-0OkKAM"
//        }
//    """.trimIndent()
//    private val kid = 1
//    private val userId = 15138791
//
//    private val jwsHeader = JWSHeader.Builder(JWSAlgorithm.ES256)
//        .build()
//
//    private val signer: ECDSASigner = ECDSASigner(ECKey.parse(jsonWebKey))
//    private val rnd = Random()
//
//    override fun invoke(): String {x
//        val claims = JWTClaimsSet.Builder()
//            .audience("usr")
//            .jwtID(rnd.nextLong().toString(Character.MAX_RADIX))
//            .issueTime(Date.from(Instant.now()))
//            .claim("uid", userId)
//            .build()
//        return SignedJWT(jwsHeader, claims).run {
//            sign(signer)
//            serialize().also {
//                println("JWT=$it")
//            }
//        }
//    }
//}
//
//fun main() {
//    val tokenGen = TokenGenerator()
////    println(tokenGen())
//    val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build()
//    val req = HttpRequest.newBuilder()
//        .uri(URI.create("https://bitzlato.com/api2/p2p/public/exchange/dsa/?lang=ru&limit=20&skip=0&type=purchase&currency=RUB&cryptocurrency=BTC&isOwnerVerificated=false&isOwnerTrusted=false&isOwnerActive=false"))
//        .timeout(Duration.ofSeconds(5))
//        .header("Authorization", "Bearer ${tokenGen()}")
//        .GET()
//        .build()
//    println("Request=$req")
//    client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
//        .thenApply { response: HttpResponse<String?> ->
//            println(response.statusCode())
//            response
//        }
//        .thenApply { obj: HttpResponse<String?> -> obj.body() }
//        .thenAccept { x: String? -> println(x) }
//        .get()
//}

//    driver.get("https://bitzlato.com/api2/p2p/public/exchange/dsa/?lang=ru&limit=20&skip=0&type=purchase&currency=RUB&cryptocurrency=BTC&isOwnerVerificated=false&isOwnerTrusted=false&isOwnerActive=false")


object Bitzlato : Exchange {
    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> = listOf()

    override fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val fetchTasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (orderType in OrderType.values()) {
                    for (page in 0 until 4) {
                        fetchTasks.add {
                            val url = requestUrl(token, currency, orderType, 20, page * 20)
                            val data = NetworkUtils.getRequest(url)
                            return@add parseResponse(token, currency, orderType, data).groupBy {
                                Setup(token, currency, this, it.paymentMethod!!, orderType)
                            }
                        }
                    }
                }
            }
        }
        return fetchTasks
    }

    fun parseResponse(token: Token, currency: Currency, orderType: OrderType, data: String): List<Offer> {
        return ObjectMapper()
            .readTree(data)["data"]
            .map { entry ->
                Offer(
                    entry["rate"].asDouble(),
                    token,
                    orderType,
                    entry["limitCryptocurrency"]["max"].asDouble(),
                    entry["limitCurrency"]["min"].asDouble(),
                    entry["limitCurrency"]["max"].asDouble(),
                    entry["owner"].asText(),
                    null,
                    null,
                    true,
                    "https://bitzlato.com/p2p" +
                            "/${if (orderType == OrderType.BUY) "buy" else "sell"}" +
                            "-${tokenToId[token]?.lowercase()}" +
                            "-${currencyToId[currency]?.lowercase()}",
                    idToPaymentMethod[entry["paymethodId"].asInt()],
                    this
                )
            }.filter { it.paymentMethod != null }
    }

    fun requestUrl(token: Token, currency: Currency, orderType: OrderType, pageSize: Int, offset: Int) =
        "https://bitzlato.bz/api2/p2p/public/exchange/dsa/" +
                "?lang=ru" +
                "&limit=$pageSize" +
                "&skip=$offset" +
                "&type=${if (orderType == OrderType.BUY) "purchase" else "selling"}" +
                "&currency=${currencyToId[currency]}" +
                "&cryptocurrency=${tokenToId[token]}" +
                "&isOwnerVerificated=true" +
                "&isOwnerTrusted=true" +
                "&isOwnerActive=true"

    val tokenToId = mapOf(
        Token.BTC to "BTC",
        Token.USDT to "USDT",
        Token.LTC to "LTC",
        Token.BCH to "BCH",
        Token.ETH to "ETH",
        Token.USDC to "USDC",
    )

    private val currencyToId = mapOf(
        Currency.RUB to "RUB",
    )

    private val idToPaymentMethod = mapOf(
        3547 to PaymentMethod.SBERBANK,
        443 to PaymentMethod.TINKOFF,
        336 to PaymentMethod.BANK_TRANSFER,
        441 to PaymentMethod.ALFA_BANK,
        452 to PaymentMethod.RAIFAIZEN,
        453 to PaymentMethod.ROCKETBANK,
        462 to PaymentMethod.POSTBANK,
        1168 to PaymentMethod.BANK_TRANSFER,
        454 to PaymentMethod.RUSSIANSTANDARD,
        65 to PaymentMethod.MTSBANK,
        456 to PaymentMethod.GAZPROMBANK,
        465 to PaymentMethod.URALSIB,
        463 to PaymentMethod.URALSIB,
        68 to PaymentMethod.MOSCOW_BANK,
        444 to PaymentMethod.QIWI,
        8802 to PaymentMethod.SBP_TRANSFER,
        8975 to PaymentMethod.YANDEXMONEY,
        447 to PaymentMethod.YANDEXMONEY,
        9163 to PaymentMethod.HOMECREDIT,
        1113 to PaymentMethod.OTKRITIE,
        3845 to PaymentMethod.SOVCOMBANK,
        3846 to PaymentMethod.ROSBANK,
        3850 to PaymentMethod.URALSIB,
        1173 to PaymentMethod.PAYEER,
        9215 to PaymentMethod.OTPBANK,
        9170 to PaymentMethod.ROSSELHOZBANK,
        3847 to PaymentMethod.UNICREDIT,
        9 to PaymentMethod.CITIBANK,
    )

    override fun supportedTokens(): Array<Token> {
        return tokenToId.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return idToPaymentMethod.values.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToId.keys.toTypedArray()
    }

    override fun name(): String {
        return "Bitzlato"
    }
}

