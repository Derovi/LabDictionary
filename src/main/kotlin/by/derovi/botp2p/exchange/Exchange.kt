package by.derovi.botp2p.exchange

interface Exchange {
    fun getFetchTasks(): List<() -> Map<Setup, List<Offer>>> {
        val tasks = mutableListOf<() -> Map<Setup, List<Offer>>>()
        for (token in supportedTokens()) {
            for (currency in supportedCurrencies()) {
                for (paymentMethod in supportedPaymentMethods()) {
                    tasks.add {
                        val result = mutableMapOf<Setup, MutableList<Offer>>()
                        result.getOrPut(
                            Setup(
                                token,
                                currency,
                                this,
                                paymentMethod,
                                OrderType.BUY,
                            ),
                        ) { mutableListOf() }.addAll(
                            fetch(OrderType.BUY, token, currency, paymentMethod)
                                .filter { it.completeRate > 0 }.filter { it.maxLimit > 10 },
                        )

                        result.getOrPut(
                            Setup(
                                token,
                                currency,
                                this,
                                paymentMethod,
                                OrderType.SELL,
                            ),
                        ) { mutableListOf() }.addAll(
                            fetch(OrderType.SELL, token, currency, paymentMethod)
                                .filter { it.completeRate > 0 }.filter { it.maxLimit > 10 },
                        )
                        return@add result
                    }
                }
            }
        }
        return tasks
    }
    fun fetch(orderType: OrderType, token: Token, currency: Currency, paymentMethod: PaymentMethod): List<Offer>
    fun supportedTokens(): Array<Token>
    fun supportedPaymentMethods(): Array<PaymentMethod>
    fun supportedCurrencies(): Array<Currency>
    fun name(): String
}