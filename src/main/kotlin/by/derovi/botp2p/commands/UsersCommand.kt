package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.BanksDialog
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.UserRepository
import by.derovi.botp2p.services.DialogService
import by.derovi.botp2p.services.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class UsersCommand : Command {
    override val name = "/users"
    override val role = Role.ADMIN

    @Autowired
    lateinit var dialogService: DialogService

    @Autowired
    lateinit var userRepository: UserRepository

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                for (serviceUser in userRepository.findAll()) {
                    append("#${serviceUser.userId}")
                    append(" @").append(serviceUser.login).append(" ")
                    append(serviceUser.role.name)
                    if (serviceUser.role != Role.UNSUBSCRIBED && serviceUser.role != Role.ADMIN) {
                        append(" до ${Utils.formatDate(serviceUser.subscribedUntil)}")
                    }
                    append("\n")
                }
                toString()
            }
        )
    }
}
