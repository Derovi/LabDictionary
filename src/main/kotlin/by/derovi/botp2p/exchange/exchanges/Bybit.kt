package by.derovi.botp2p.exchange.exchanges

import by.derovi.botp2p.exchange.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.entity.ContentType

object Bybit : Exchange {
    override fun fetch(
        orderType: OrderType,
        token: Token,
        currency: Currency,
        paymentMethod: PaymentMethod
    ): List<Offer> {
        val result = mutableListOf<Offer>()
        val payload = requestPayload(token, currency, orderType, paymentMethod, 1)
        val data = NetworkUtils.postRequest("https://api2.bybit.com/spot/api/otc/item/list", payload, ContentType.APPLICATION_FORM_URLENCODED)
        result.addAll(parseResponse(token, currency, paymentMethod, orderType, data))
        result.forEach {
            it.paymentMethod = paymentMethod
            it.exchange = this
        }
        return result
    }

    fun parseResponse(token: Token, currency: Currency, paymentMethod: PaymentMethod, orderType: OrderType, data: String): List<Offer> =
        ObjectMapper().readTree(data)["result"]["items"].map {
            Offer(
                it["price"].asDouble(),
                token,
                orderType,
                it["lastQuantity"].asDouble(),
                it["minAmount"].asDouble(),
                it["maxAmount"].asDouble(),
                it["nickName"].asText(),
                it["recentExecuteRate"].asInt(),
                it["recentOrderNum"].asInt(),
                it["isOnline"].asBoolean(),
                "https://www.bybit.com/fiat/trade/otc/" +
                        "?actionType=${if (orderType == OrderType.BUY) 1 else 0}" +
                        "&token=${tokenToCode[token]}" +
                        "&fiat=${currencyToCode[currency]}" +
                        "&paymentMethod=${paymentMethodToCode[paymentMethod]}"
            )
        }

    fun requestPayload(token: Token, currency: Currency, orderType: OrderType, paymentMethod: PaymentMethod, page: Int) =
        "userId=" +
        "&tokenId=${tokenToCode[token]}" +
        "&currencyId=${currencyToCode[currency]}" +
        "&payment=${paymentMethodToCode[paymentMethod]}" +
        "&side=${if (orderType == OrderType.BUY) 1 else 0}" +
        "&size=10" +
        "&page=$page" +
        "&amount="

    val tokenToCode = mapOf(
        Token.BTC to "BTC",
        Token.USDC to "USDC",
        Token.USDT to "USDT",
        Token.ETH to "ETH",
    )

    private val currencyToCode = mapOf(
        Currency.RUB to "RUB",
    )

    private val paymentMethodToCode = mapOf(
        PaymentMethod.BANK_TRANSFER to 14,
        PaymentMethod.FPS to 27,
        PaymentMethod.MTSBANK to 44,
        PaymentMethod.OTPBANK to 49,
        PaymentMethod.PAYEER to 51,
        PaymentMethod.PERFECT_MONEY to 56,
        PaymentMethod.POSTBANK to 59,
        PaymentMethod.QIWI to 62,
        PaymentMethod.RAIFAIZEN to 64,
        PaymentMethod.ROSSELHOZBANK to 66,
        PaymentMethod.SPORTBANK to 72,
        PaymentMethod.TINKOFF to 75,
        PaymentMethod.YANDEXMONEY to 88,
        PaymentMethod.HOMECREDIT to 102,
        PaymentMethod.ABSOLUTBANK to 173,
        PaymentMethod.ROSBANK to 185,
        PaymentMethod.CITIBANK to 231,
        PaymentMethod.PARITETBANK to 333,
        PaymentMethod.MTBANK to 332,
        PaymentMethod.SBERBANK to 377,
        PaymentMethod.GAZPROMBANK to 378,
        PaymentMethod.ALFA_BANK to 379,
        PaymentMethod.OTKRITIE to 380,
        PaymentMethod.VTB to 381,
        PaymentMethod.SBP_TRANSFER to 382
    )

    override fun supportedTokens(): Array<Token> {
        return tokenToCode.keys.toTypedArray()
    }

    override fun supportedPaymentMethods(): Array<PaymentMethod> {
        return paymentMethodToCode.keys.toTypedArray()
    }

    override fun supportedCurrencies(): Array<Currency> {
        return currencyToCode.keys.toTypedArray()
    }

    override fun name(): String {
        return "Bybit"
    }
}