package by.derovi.botp2p.services

import by.derovi.botp2p.commands.BundleCommand
import by.derovi.botp2p.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class NotificationsService {
    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var dialogService: DialogService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var commandService: CommandService

    private var userIdToLastBundles = mutableMapOf<Long, List<BundleKey>>()

    @Scheduled(fixedRate = 5000)
    @Transactional
    fun makeNotifications() {
        val newUserIdToLastBundles = mutableMapOf<Long, List<BundleKey>>()
        for (user in userRepository.findAll()) {
            val threshold = user.userSettings.notificationThreshold
            if (!user.userSettings.notificationsOn
                || (userService.userIdToLastAction[user.userId] ?: 0) + 20 * 1000L > System.currentTimeMillis()
                || dialogService.isDialogActive(user.userId)
            ) {
                continue
            }
            val newBundleKeys = mutableListOf<BundleKey>()
            var notificationSent = false
            for ((index, bundle) in (bundlesService.userToBundleSearchResulTT[user.userId]
                ?: listOf()).withIndex()) {
                if (bundle.spreadWithFee >= threshold / 100.0) {
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
                        if (user.login?.endsWith("deroviAdmin") == true) {
                            println("sent notification ${System.currentTimeMillis()} ${bundlesService.lastUpdateTime}")
                        }
                        commandService.use(
                            "/bundle?${BundleCommand.bundleKeyToCommandArgs(bundleKey)}&false", botUser
                        )
                    }
                }
            }
            newUserIdToLastBundles[user.userId] = newBundleKeys
        }
        userIdToLastBundles = newUserIdToLastBundles
    }
}
