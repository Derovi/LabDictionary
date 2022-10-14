package by.derovi.botp2p.services

import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.transaction.Transactional
import kotlin.concurrent.thread

@Service
class NotificationsService {
    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var commandService: CommandService

    data class BundleKey(
        val currency: Currency,
        val buyToken: Token,
        val sellToken: Token,
        val buyExchange: Exchange,
        val sellExchange: Exchange
    )

    private var userIdToLastBundles = mutableMapOf<Long, List<BundleKey>>()

    @Scheduled(fixedRate = 5000)
    @Transactional
    fun makeNotifications() {
        val newUserIdToLastBundles = mutableMapOf<Long, List<BundleKey>>()
        for (user in userRepository.findAll()) {
            val newBundleKeys = mutableListOf<BundleKey>()
            var notificationSent = false
            for ((index, bundle) in (bundlesService.userToBundleSearchResulTT[user.userId]
                ?: listOf()).withIndex()) {
                if (bundle.spreadWithFee >= user.userSettings.notificationThreshold / 100.0) {
                    val bundleKey = BundleKey(
                        bundle.currency,
                        bundle.buyToken,
                        bundle.sellToken,
                        bundle.buyExchange,
                        bundle.sellExchange
                    )
                    newBundleKeys.add(bundleKey)
                    if (userIdToLastBundles[user.userId]?.contains(bundleKey) != true && !notificationSent) {
                        notificationSent = true
                        val botUser = userService.getBotUserById(user.userId)
                        botUser.sendMessage("Найдена связка на <b>${Utils.normalizeSpread(bundle.spreadWithFee)}</b>%")
                        commandService.use("/bundles?$index&true", botUser)
                    }
                }
            }
            newUserIdToLastBundles[user.userId] = newBundleKeys
        }
        userIdToLastBundles = newUserIdToLastBundles
    }
}
