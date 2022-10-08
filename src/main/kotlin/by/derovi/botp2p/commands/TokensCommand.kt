package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.ExchangesDialog
import by.derovi.botp2p.dialogs.TokensDialog
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class TokensCommand : Command {
    override val name = "/tokens"
    override val role = Role.STANDARD

    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var dialogService: DialogService

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("<b>Укажите один или несколько токенов</b>\n")
                append("Доступны: <i>${Token.values().map(Token::name).joinToString(", ")}</i>\n")
                append("Установлены: <i>${user.serviceUser.userSettings.tokens.joinToString(", ")}</i>\n")
                append("<i>Токены нужно перечислять через запятую</i>\nПример: <b>USDT, BTC</b>")
                toString()
            }
        )

        dialogService.startDialog(user, TokensDialog::class.java)
    }
}
