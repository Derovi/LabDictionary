package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import org.springframework.stereotype.Component

@Component
class MeCommand : Command {
    override val name = "/me"
    override val role = Role.UNSUBSCRIBED

    override fun use(user: BotUser, vararg args: String) {
        val message = user.id.toString() + "\n" + user.serviceUser.login
        user.sendMessage(message)
    }
}
