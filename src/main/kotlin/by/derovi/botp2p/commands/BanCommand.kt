package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Maker
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class BanCommand : Command {
    override val name = "/ban"
    override val role = Role.STANDARD

    @Autowired
    lateinit var context: ApplicationContext

    override fun use(user: BotUser, vararg args: String) {
        if (args.size < 2) {
            return
        }
        user.serviceUser.userSettings.banned.add(Maker(args[0], args[1]))
        user.sendMessage("<b>${args[0]}</b> на <b>${args[1]}</b> забанен!")
        user.searchBundles()
        context.getBean(CommandService::class.java).back(user)
    }
}