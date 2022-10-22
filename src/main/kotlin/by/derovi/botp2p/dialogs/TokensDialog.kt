package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SearchSettingsCommand
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.library.checkIfSelected
import by.derovi.botp2p.model.SearchSettings
import by.derovi.botp2p.model.SearchSettingsRepository
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
@Scope("prototype")
class TokensDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var searchSettingsRepository: SearchSettingsRepository

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var buttonsService: ButtonsService

    lateinit var searchSettings: SearchSettings

    fun printText(user: BotUser) {
        user.sendMessage(buildString {
            append("\uD83E\uDE99 Токены\n")
            append("Установлены: <code>${
                Token.values()
                    .filter { it in searchSettings.tokens }
                    .map(Token::name).joinToString(", ")}</code>\n"
            )
            append("Доступны: <code>${Token.values().map(Token::name).joinToString(", ")}</code>\n")
            append("<i>Введите токены через запятую или выберите их кнопками</i>")
            toString()
        },
            with(InlineKeyboardMarkup.builder()) {
                Token.values().asSequence().chunked(4).map {
                    it.map { token ->
                        val selected = token in searchSettings.tokens
                        InlineKeyboardButton.builder()
                            .text(token.name.checkIfSelected(selected))
                            .callbackData("gui:" + searchSettings.tokens.toMutableSet().apply {
                                if (selected) {
                                    remove(token)
                                } else {
                                    add(token)
                                }
                            }.joinToString(","))
                            .build()
                    }
                }.forEach(::keyboardRow)
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun start(user: BotUser, args: List<String>) {
        searchSettings = SearchSettingsCommand.getSettingsByArgs(user.serviceUser.userSettings, args)
        printText(user)
    }

    override fun update(user: BotUser): Boolean {
        val (text, gui) = if (user.message.startsWith("gui:"))
            user.message.substringAfter("gui:") to true else user.message to false
        val currentTokens = searchSettings.tokens

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
        searchSettings.tokens = newTokens
        searchSettingsRepository.save(searchSettings)
        if (gui) {
            printText(user)
            return true
        }
        user.sendMessage("\uD83E\uDE99 Установлены токены [<code>${newTokens.map(Token::name)
            .joinToString(", ")}</code>]")
        commandService.back(user)
        return false
    }
}