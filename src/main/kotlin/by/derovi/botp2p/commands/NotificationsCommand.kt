package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.NotificationsDialog
import by.derovi.botp2p.dialogs.TokensDialog
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class NotificationsCommand : Command {
    @Autowired
    lateinit var dialogService: DialogService

    override val name = "/notifications"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("<b>При нахождении связки со спредом не менее указанного, вам будет отправляться уведомление</b>\n")
                append("Текущее значение: [<b>${user.serviceUser.userSettings.notificationThreshold}%</b>]\n")
                append("<i>Отправьте сообщение с необходимым спредом в процентах</i>\nПример: <b>1.5</b>")
                toString()
            }
        )

        dialogService.startDialog(user, NotificationsDialog::class.java)
    }
}
