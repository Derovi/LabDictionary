package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.MinValueDialog
import by.derovi.botp2p.dialogs.ModeDialog
import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MinValueCommand : Command {
    override val name = "/setminvalue"
    override val role = Role.STANDARD

    @Autowired
    lateinit var dialogService: DialogService

    override fun use(user: BotUser, vararg args: String) {
        dialogService.startDialog(user, MinValueDialog::class.java)
    }
}
