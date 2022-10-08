package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class TokensDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService
    override fun start(user: BotUser) {}

    override fun update(user: BotUser): Boolean {
        val text = user.message
        val currentTokens = user.serviceUser.userSettings.tokens
        val inputTokens = text.split(Regex("[^a-zA-Z0-9]+"))

        val newTokens = mutableListOf<Token>()
        for (tokenName in inputTokens) {
            val token = Token.values().find { it.name.equals(tokenName, ignoreCase = true) }
            if (token != null) {
                newTokens.add(token)
            } else {
                user.sendMessage(
                    with(StringBuilder()) {
                        append("<b>Токена \"$tokenName\" не существует!</b>\n")
                        append("Доступны: <i>${Token.values().map(Token::name).joinToString(", ")}</i>\n")
                        append("Установлены: <i>${currentTokens.joinToString(", ")}</i>\n")
                        append("<i>Токены нужно перечислять через запятую</i>\nПример: <b>USDT, BTC</b>")
                        toString()
                    }
                )
                commandService.back(user)
                return false
            }
        }
        user.serviceUser.userSettings.tokens = newTokens
        user.sendMessage("Установлены следующие токены: <b>${newTokens.map(Token::name).joinToString(", ")}</b>")
        commandService.back(user)
        return false
    }
}