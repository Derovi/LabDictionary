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
        if (botUser.serviceUser.subscribedUntil >= System.currentTimeMillis()) {
            botUser.serviceUser.role = Role.UNSUBSCRIBED
//            commandService.adjustSettingsAccordingToPermissions(botUser)
            botUser.sendMessage(
                "<b>Ваша подписка <i>${botUser.serviceUser.role.readableName}</i> закончилась!</b>",
                InlineKeyboardMarkup.builder().keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("Продлить").callbackData("/subscription").build())).build()
            )
        }
    }

    @Scheduled(cron = "0 0 */1 * * *")
    fun updateAllSubscriptions() {
        userRepository.findAll().map { userService.getBotUserById(it.userId) }.forEach(::update)
    }

    fun subscribe(id: Long, role: Role, days: Int) {
        val millis = days * 24L * 60 * 60 * 1000
        userRepository.findById(id).ifPresent {
            if (it.role == role) {
                it.subscribedUntil += millis
            } else {
                it.role = role
                it.subscribedUntil = System.currentTimeMillis() + millis
            }
            userRepository.save(it)
            if (it.referPromo == null) {
                val promo = promoService.createPromo(
                    promoService.referDiscount,
                    referId = id
                )
                println(promo.id)
//                it.referPromo = promo
            }
            userRepository.save(it)
        }
    }
}