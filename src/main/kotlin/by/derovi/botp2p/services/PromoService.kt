package by.derovi.botp2p.services

import by.derovi.botp2p.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
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

    @Autowired
    lateinit var userRepository: UserRepository
    @Autowired
    lateinit var userService: UserService

    val referReward = 0.2
    val referDiscount = 0.0
    fun getPromo(id: String): Promo? = promoRepository.findById(id).orElse(null)

    val prices = mapOf(
        Role.STANDARD to mapOf(
            SubscriptionDuration.WEEK to 49,
            SubscriptionDuration.MONTH to 149,
            SubscriptionDuration.SEASON to 379,
            SubscriptionDuration.HALF_YEAR to 669,
            SubscriptionDuration.YEAR to 1069
        ),
        Role.ADVANCED to mapOf(
            SubscriptionDuration.WEEK to 59,
            SubscriptionDuration.MONTH to 179,
            SubscriptionDuration.SEASON to 459,
            SubscriptionDuration.HALF_YEAR to 799,
            SubscriptionDuration.YEAR to 1279
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

    fun promoUsed(
        promo: Promo,
        promoPrice: Long,
        role: Role,
        duration: SubscriptionDuration
    ) {
        val referId = promo.referId ?: return
        val refer = userService.getBotUserById(referId)
        val amount = (promoPrice * referReward).toInt()
        refer.serviceUser.referBonus += amount
        refer.serviceUser.referNumber++
        refer.sendMessageWithBackButton(
            buildString {
                append("\uD83D\uDC65 Пользователь оплатил подписку <b>${role.readableName}</b> " +
                        "на <b>${duration.readableName2}</b> по вашей реферальной ссылке!\n")
                append("Вам зачислено <b>$amount usdt</b> бонусами!")
            }
        )
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