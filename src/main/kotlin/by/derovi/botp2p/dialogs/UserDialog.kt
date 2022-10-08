package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class UserDialog : Dialog {

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var userRepository: UserRepository
    override fun start(user: BotUser) {}

    override fun update(user: BotUser): Boolean {
        val id = user.message.toLongOrNull()
        if (id == null) {
            user.sendMessage("<b>\"${user.message}\" не является числом!</b>")
            commandService.back(user)
            return false
        }
        if (!userRepository.existsById(id)) {
            user.sendMessage("<b>Пользователь с id <i>$id</i> не найден!</b>")
            commandService.back(user)
            return false
        }
        val serviceUser = userRepository.findById(id).get()
        user.sendMessage(
            with(StringBuilder()) {
                append("<b>${serviceUser.userId}</b>\n")
                append("${serviceUser.login}\n")
                append("<b>${serviceUser.role.name}</b>")
                if (serviceUser.role != Role.UNSUBSCRIBED && serviceUser.role != Role.ADMIN) {
                    append(" до <b>${Utils.formatDate(serviceUser.subscribedUntil)}</b>")
                }
                toString()
            },
            InlineKeyboardMarkup.builder()
              .keyboardRow(mutableListOf(
                InlineKeyboardButton.builder().text("Подписать").callbackData("/subscribe#${serviceUser.userId}").build(),
                InlineKeyboardButton.builder().text("Заблокировать").callbackData("/ban#${serviceUser.userId}").build()
            )).keyboardRow(mutableListOf(
                InlineKeyboardButton.builder().text("Пользователи").callbackData("/users").build(),
                InlineKeyboardButton.builder().text("На главную").callbackData("/start").build(),
            )).build()
        )
        return true
    }
}
