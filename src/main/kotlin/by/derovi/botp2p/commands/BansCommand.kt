package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class BansCommand : Command {
    override val name = "/bans"
    override val role = Role.STANDARD

    @Autowired
    lateinit var context: ApplicationContext

    fun unbanLink(name: String, exchange: String) = "<a href=\"https://t.me/deroviBot?start=${String(Utils.base62.encode("/unban?$name&$exchange".toByteArray()))}\">[разблокировать]</a>"
    override fun use(user: BotUser, vararg args: String) {
        user.sendMessageWithBackButton(
            with(StringBuilder()) {
                append("Забаненные мейкеры:\n\n")
                for (maker in user.serviceUser.userSettings.banned) {
                    append("<b>${maker.name}</b> on <b>${maker.exchange}</b>  ${unbanLink(maker.name, maker.exchange)}\n")
                }
                toString()
            }
        )
    }
}