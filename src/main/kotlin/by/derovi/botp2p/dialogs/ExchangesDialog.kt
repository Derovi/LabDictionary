package by.derovi.botp2p.dialogs

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
class ExchangesDialog : Dialog {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var commandService: CommandService
    override fun start(user: BotUser) {}

    override fun update(user: BotUser): Boolean {
        val text = user.message
        val currentExchanges = user.serviceUser.userSettings.exchanges
        val inputExchanges = text.split(Regex("[^a-zA-Z0-9]+"))

        val availableExchanges = bundleSearch.exchanges.map(Exchange::name)
        val newExchanges = mutableListOf<String>()
        for (exchangeName in inputExchanges) {
            val exchange = availableExchanges.find { it.equals(exchangeName, ignoreCase = true) }
            if (exchange != null) {
                newExchanges.add(exchange)
            } else {
                user.sendMessage(
                    with(StringBuilder()) {
                        append("<b>Биржи \"$exchangeName\" не существует!</b>\n")
                        append("Доступны: <i>${availableExchanges.joinToString(", ")}</i>\n")
                        append("Установлены: <i>${currentExchanges.joinToString(", ")}</i>\n")
                        append("<i>Биржи нужно перечислять через запятую</i>\nПример: <b>Huobi, Binance</b>")
                        toString()
                    }
                )
                commandService.back(user)
                return false
            }
        }
        user.serviceUser.userSettings.exchanges = newExchanges
        user.sendMessage("Установлены следующие биржи: <b>${newExchanges.joinToString(", ")}</b>")
        commandService.back(user)
        return false
    }
}