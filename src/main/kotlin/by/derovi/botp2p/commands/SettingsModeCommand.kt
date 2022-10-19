package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.dialogs.SettingsModeDialog
import by.derovi.botp2p.exchange.TradingMode
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.model.SettingsMode
import by.derovi.botp2p.services.DialogService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SettingsModeCommand : Command {
    override val name = "/settingsmode"
    override val role = Role.STANDARD

    @Autowired
    lateinit var dialogService: DialogService

    override fun use(user: BotUser, vararg args: String) {
        dialogService.startDialog(user, SettingsModeDialog::class.java)
    }

    override fun removeAffect(user: BotUser) {
        user.serviceUser.userSettings.settingsMode = SettingsMode.STANDARD
    }
}
