package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.*
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ManagedCommand : Command {
    @Autowired
    lateinit var context: ApplicationContext

    override val name = "/managedcommand"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        val commandService = context.getBean(CommandService::class.java)
        val id = args.firstOrNull()?.toLongOrNull() ?: return
        val command = commandService.getManagedCommand(id)?.command ?: return
        commandService.use(
            command,
            user,
        )
    }
}
