package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.BundleSearch
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.CommandService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.transaction.Transactional
import kotlin.math.round

@Component
class SpotCommand : Command {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    override val name = "/spot"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        val before = user.serviceUser.userSettings.useSpot
        user.serviceUser.userSettings.useSpot = !before
        user.bot.updateService.userService.userRepository.save(user.serviceUser)

        user.sendMessage("Торговля через спот <b>${ if (before) "Выключена" else "Включена"}</b>")

        user.bot.updateService.commandService.back(user)
    }

    override fun removeAffect(user: BotUser) {
        user.serviceUser.userSettings.useSpot = false
    }
}
