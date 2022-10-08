package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Maker
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class UnbanCommand : Command {
    override val name = "/unban"
    override val role = Role.STANDARD

    @Autowired
    lateinit var context: ApplicationContext

    override fun use(user: BotUser, vararg args: String) {
        if (args.size < 2) {
            return
        }
        if (user.serviceUser.userSettings.banned.contains(Maker(args[0], args[1]))) {
            user.serviceUser.userSettings.banned.remove(Maker(args[0], args[1]))
            user.sendMessage("<b>${args[0]}</b> на <b>${args[1]}</b> разблокирован!")
        } else {
            user.sendMessage("<b>${args[0]}</b> на <b>${args[1]}</b> не забанен!")
        }
        context.getBean(CommandService::class.java).back(
            user,
            if (user.serviceUser.userSettings.banned.isEmpty()) 2 else 1
        )
    }
}