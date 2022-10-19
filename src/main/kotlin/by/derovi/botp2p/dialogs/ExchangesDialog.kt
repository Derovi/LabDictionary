package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SearchSettingsCommand
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
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
class ExchangesDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var searchSettingsRepository: SearchSettingsRepository

    @Autowired
    lateinit var commandService: CommandService

    @Autowired
    lateinit var buttonsService: ButtonsService

    lateinit var searchSettings: SearchSettings

    override fun start(user: BotUser, args: List<String>) {
        searchSettings = SearchSettingsCommand.getSettingsByArgs(user.serviceUser.userSettings, args)

        val exchanges = bundleSearch.commonExchanges.map(Exchange::name)
        val chosenExchanges = exchanges.filter { it in searchSettings.exchanges }
        user.sendMessageWithBackButton(
            buildString {
                append("\uD83D\uDCB1 Биржи\n")
                append("Установлены: <code>${
                    chosenExchanges.joinToString(", ")}</code>\n"
                )
                append("Доступны: <code>${exchanges.joinToString(", ")}</code>\n")
                append("<i>Введите биржи через запятую</i>")
                toString()
            }
        )
    }

    override fun update(user: BotUser): Boolean {
        val text = user.message
        val currentExchanges = searchSettings.exchanges
        val inputExchanges = text.split(Regex("[^a-zA-Z0-9]+"))

        val availableExchanges = bundleSearch.commonExchanges.map(Exchange::name)
        val newExchanges = mutableListOf<String>()
        for (exchangeName in inputExchanges) {
            val exchange = availableExchanges.find { it.equals(exchangeName, ignoreCase = true) }
            if (exchange != null) {
                newExchanges.add(exchange)
            } else {
                user.sendMessage("\uD83D\uDCB1 Биржи <b>\"$exchangeName\"</b> не существует!")
                commandService.back(user)
                return false
            }
        }
        searchSettings.exchanges = newExchanges
        searchSettingsRepository.save(searchSettings)
        user.sendMessage("\uD83E\uDE99 Установлены биржи " +
                "[<code>${newExchanges.joinToString(", ")}</code>]")
        commandService.back(user)
        return false
    }
}