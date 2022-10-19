package by.derovi.botp2p.services

import by.derovi.botp2p.model.Promo
import by.derovi.botp2p.model.PromoRepository
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SubscriptionDuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.annotation.PostConstruct
import kotlin.math.ceil
import kotlin.random.Random

val size = 6
val charPool = ('0'..'9') + ('A'..'Z') + ('a'..'z')
private fun randomPromoIdWithoutValidation() = (0 until size)
    .map { Random.nextInt(charPool.size) }
    .map(charPool::get)
    .joinToString("")

@Service
class PromoService {
    @Autowired
    lateinit var promoRepository: PromoRepository

    val referReward = 0.1
    val referDiscount = 0.2
    fun getPromo(id: String): Promo? = promoRepository.findById(id).orElse(null)

    val prices = mapOf(
        Role.STANDARD to mapOf(
            SubscriptionDuration.WEEK to 30,
            SubscriptionDuration.MONTH to 100,
            SubscriptionDuration.SEASON to 200,
            SubscriptionDuration.YEAR to 300
        )
    )

    fun pricesWithPromo(discount: Double): MutableMap<Role, MutableMap<SubscriptionDuration, Int>> {
        val newPrices = mutableMapOf<Role, MutableMap<SubscriptionDuration, Int>>()
        for ((role, durationMap) in prices) {
            val newDurationMap = mutableMapOf<SubscriptionDuration, Int>()
            for ((subscriptionDuration, price) in durationMap) {
                newDurationMap[subscriptionDuration] = ceil(price * (1 - discount)).toInt()
            }
            newPrices[role] = newDurationMap
        }
        return newPrices
    }

    companion object {
        fun percentToReadable(percent: Double) = (percent * 100).toInt().toString() + "%"
    }

    fun createPromo(
        discount: Double,
        id: String? = null, // if null choose random free
        referId: Long? = null,
        expirationDate: Long? = null
    ): Promo {
        if (id != null) {
            return promoRepository.save(Promo(id, discount, referId, expirationDate))
        }
        while (true) {
            val randomId = randomPromoIdWithoutValidation()
            if (!promoRepository.existsById(randomId)) {
                val promo = Promo(randomId, discount, referId, expirationDate)
                promoRepository.save(promo)
                return promo
            }
        }
    }

    @PostConstruct
    fun createDefaultPromos() {
        createPromo(
            0.5,
            "SHAPA"
        )
    }

    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun checkExpiredPromos() {
        promoRepository.deleteByExpirationDateBefore(System.currentTimeMillis())
    }

    val size = 6
    val charPool = ('0'..'9') + ('A'..'Z') + ('a'..'z')
    private fun randomPromoIdWithoutValidation() = (0 until size)
        .map { Random.nextInt(charPool.size) }
        .map(charPool::get)
        .joinToString("")
}