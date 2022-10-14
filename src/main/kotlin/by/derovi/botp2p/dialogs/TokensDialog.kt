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

    override fun start(user: BotUser) {
        user.sendMessageWithBackButton(buildString {
            append("\uD83E\uDE99 Токены\n")
            append("Установлены: <code>${
                Token.values()
                    .filter { it in user.serviceUser.userSettings.tokens }
                    .map(Token::name).joinToString(", ")}</code>\n"
            )
            append("Доступны: <code>${Token.values().map(Token::name).joinToString(", ")}</code>\n")
            append("<i>Введите токены через запятую</i>")
            toString()
        })
    }

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
                user.sendMessage("\uD83E\uDE99 Токена <b>\"$tokenName\"</b> не существует!")
                commandService.back(user)
                return false
            }
        }
        user.serviceUser.userSettings.tokens = newTokens
        user.sendMessage("\uD83E\uDE99 Установлены токены [<code>${newTokens.map(Token::name)
            .joinToString(", ")}</code>]")
        commandService.back(user)
        return false
    }
}