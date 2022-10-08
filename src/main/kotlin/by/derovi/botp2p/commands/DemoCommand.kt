package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.stereotype.Component

@Component
class DemoCommand : Command {
    override val name = "/demo"
    override val role = Role.UNSUBSCRIBED

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessage("<b>Команда пока не работает</b>")
    }
}
