package by.derovi.botp2p.exchange

class Offer(val price: Double,
            val token: Token,
            val orderType: OrderType,
            val available: Double /* token */,
            val minLimit: Double,
            val maxLimit: Double,
            val username: String,
            val completeRate: Int,
            val completeCount: Int,
            val isOnline: Boolean,
            val link: String,
            var paymentMethod: PaymentMethod? = null,
            var exchange: Exchange? = null)