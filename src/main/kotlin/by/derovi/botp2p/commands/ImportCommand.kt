package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.ImportDialog
import by.derovi.botp2p.dialogs.SettingsModeDialog
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.PaymentMethod
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.*
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.DialogService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ImportCommand : Command {
    @Autowired
    lateinit var dialogService: DialogService

    override val name = "/import"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        dialogService.startDialog(user, ImportDialog::class.java, args.toList())
    }
}
