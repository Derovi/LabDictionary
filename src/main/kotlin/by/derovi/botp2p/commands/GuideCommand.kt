package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.stereotype.Component

@Component
class GuideCommand : Command {
    override val name = "/guide"
    override val role = Role.UNSUBSCRIBED

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessage("<b>Гайдов пока нет</b>")
    }
}
