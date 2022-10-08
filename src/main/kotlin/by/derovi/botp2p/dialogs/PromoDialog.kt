package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.PromoRepository
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class PromoDialog : Dialog {

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var promoRepository: PromoRepository

    @Autowired
    lateinit var userRepository: UserRepository

    override fun start(user: BotUser) {
        user.sendMessage("Введите промокод:")
    }

    override fun update(user: BotUser): Boolean {
        val promo = promoRepository.findById(user.message).orElse(null)
        if (promo == null) {
            user.sendMessage("Промокода <b>${user.message}</b> не найдено!")
            commandService.back(user)
            return true
        }
        user.serviceUser.promo = promo
        user.sendMessage(
            "Вы использовали промокод <b>${promo.id}</b> на <b>${(promo.discount * 100).toInt()}%</b>")
        commandService.back(user)
        return true
    }
}
