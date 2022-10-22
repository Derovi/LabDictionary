package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.commands.SearchSettingsCommand
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.exchange.exchanges.Binance
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

    var idx = 0

    fun printText(user: BotUser) {
        val exchanges = bundleSearch.commonExchanges.map(Exchange::name)
        val chosenExchanges = exchanges.filter { it in searchSettings.exchanges }
        user.sendMessage(
            buildString {
                append("\uD83D\uDCB1 Биржи\n")
                append("Установлены: <code>${
                    chosenExchanges.joinToString(", ")}</code>\n"
                )
                append("Доступны: <code>${exchanges.joinToString(", ")}</code>\n")
                append("<i>Введите биржи через запятую</i>")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                exchanges.asSequence().chunked(3).map {
                    it.map { exchange ->
                        val selected = exchange in chosenExchanges
                        InlineKeyboardButton.builder()
                            .text(exchange.checkIfSelected(selected))
                            .callbackData("gui:" + chosenExchanges.toMutableSet().apply {
                                if (selected) {
                                    remove(exchange)
                                } else {
                                    add(exchange)
                                }
                            }.joinToString(","))
                            .build()
                    }
                }.forEach(::keyboardRow)
                if (idx in listOf(0, 1, 4) && chosenExchanges.contains(Binance.name())) {
                    keyboardRow(
                        mutableListOf(
                            if (searchSettings.buyMakerBinance)
                                InlineKeyboardButton.builder()
                                    .text("\uD83D\uDFE2 Покупка как Мейкер на Binance [Включено]")
                                    .callbackData("gui:buymakerbinanceoff")
                                    .build()
                            else
                                InlineKeyboardButton.builder()
                                    .text("\uD83D\uDD34 Покупка как Мейкер на Binance [Выключено]")
                                    .callbackData("gui:buymakerbinanceon")
                                    .build()
                        )
                    )
                }
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }

    override fun start(user: BotUser, args: List<String>) {
        idx = args.firstOrNull()?.toIntOrNull() ?: 0
        searchSettings = SearchSettingsCommand.getSettingsByIdx(user.serviceUser.userSettings, idx)
        printText(user)
    }

    override fun update(user: BotUser): Boolean {
        val (text, gui) = if (user.message.startsWith("gui:"))
            user.message.substringAfter("gui:") to true else user.message to false

        val newExchanges = mutableListOf<String>()

        if (text == "buymakerbinanceon") {
            searchSettings.buyMakerBinance = true
        } else if (text == "buymakerbinanceoff") {
            searchSettings.buyMakerBinance = false
        } else {
            val inputExchanges = text.split(Regex("[^a-zA-Z0-9]+"))
            val availableExchanges = bundleSearch.commonExchanges.map(Exchange::name)
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
        }
        searchSettingsRepository.save(searchSettings)
        if (gui) {
            printText(user)
            return true
        }
        user.sendMessage("\uD83E\uDE99 Установлены биржи " +
                "[<code>${newExchanges.joinToString(", ")}</code>]")
        commandService.back(user)
        return false
    }
}