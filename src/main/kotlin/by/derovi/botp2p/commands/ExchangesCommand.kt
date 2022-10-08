package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.ExchangesDialog
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ExchangesCommand : Command {
    override val name = "/exchanges"
    override val role = Role.STANDARD

    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var dialogService: DialogService

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("<b>Укажите одну или несколько бирж</b>\n")
                append("Доступны: <i>${bundleSearch.exchanges.map(Exchange::name).joinToString(", ")}</i>\n")
                append("Установлены: <i>${user.serviceUser.userSettings.exchanges.joinToString(", ")}</i>\n")
                append("<i>Биржи нужно перечислять через запятую</i>\nПример: <b>Huobi, Binance</b>")
                toString()
            }
        )

        dialogService.startDialog(user, ExchangesDialog::class.java)
    }
}
