package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.BanksDialog
import by.derovi.botp2p.dialogs.SubscribeDialog
import by.derovi.botp2p.dialogs.UserDialog
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class UserCommand : Command {
    override val name = "/user"
    override val role = Role.ADMIN

    @Autowired
    lateinit var dialogService: DialogService

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessage("<b>Введите id пользователя</b>")
        dialogService.startDialog(user, UserDialog::class.java)
    }
}
