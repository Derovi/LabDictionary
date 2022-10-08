package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.Currency
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ChooseCurrencyCommand : Command {
    override val name = "/chooseCurrency"
    override val role = Role.STANDARD

    @Autowired
    lateinit var context: ApplicationContext

    override fun use(user: BotUser, vararg args: String) {
        user.serviceUser.userSettings.chosenCurrency = Currency.values().find { it.name == args.firstOrNull() }
        user.searchBundles()
        context.getBean(CommandService::class.java).back(user)
    }
}