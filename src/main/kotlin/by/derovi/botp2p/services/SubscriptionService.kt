package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class SubscriptionService {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var promoService: PromoService

    fun update(botUser: BotUser) {
        if (botUser.serviceUser.role == Role.UNSUBSCRIBED || botUser.serviceUser.role == Role.ADMIN) {
            return
        }
        if (botUser.serviceUser.subscribedUntil < System.currentTimeMillis()) {
//            commandService.adjustSettingsAccordingToPermissions(botUser)
            botUser.sendMessage(
                "\uD83D\uDCC5 Ваша подписка <b>${botUser.serviceUser.role.readableName}</b> закончилась!",
                with(InlineKeyboardMarkup.builder()) {
                    keyboardRow(mutableListOf(InlineKeyboardButton.builder()
                        .text("\uD83D\uDC49 Продлить")
                        .callbackData("/subscription").build()))
                    build()
                }
            )
            botUser.serviceUser.role = Role.UNSUBSCRIBED
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    fun updateAllSubscriptions() {
        userRepository.findAll().map { userService.getBotUserById(it.userId) }.forEach(::update)
    }

    fun subscribe(id: Long, role: Role, days: Int) {
        val millis = days * 24L * 60 * 60 * 1000
        val user = userRepository.findById(id).orElse(null) ?: return
        if (user.role == role) {
            user.subscribedUntil += millis
        } else {
            user.role = role
            user.subscribedUntil = System.currentTimeMillis() + millis
        }
        userRepository.save(user)
        if (user.referPromo == null) {
            val promo = promoService.createPromo(
                promoService.referDiscount,
                referId = id
            )
            println(promo.id)
            user.referPromo = promo
        }
        userRepository.save(user)
    }
}