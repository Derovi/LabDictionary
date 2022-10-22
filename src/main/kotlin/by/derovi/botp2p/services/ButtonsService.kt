package by.derovi.botp2p.services

import by.derovi.botp2p.model.Maker
import by.derovi.botp2p.model.UserSettings
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Service
class ButtonsService {
    fun modeButton(settings: UserSettings) =
        InlineKeyboardButton
            .builder()
            .text("\uD83D\uDC65 Изменить режим [${settings.tradingMode.readableName}]")
            .callbackData("/mode")
            .build()

    val d = sequence<Int> {

    }

    fun bansButton(settings: UserSettings) =
        InlineKeyboardButton
            .builder()
            .text("\uD83D\uDEAB Заблокированные мейкеры [${settings.banned.size}]")
            .callbackData("/bans")
            .build()

    fun backButton() =
        InlineKeyboardButton
            .builder()
            .text("↩️ Назад")
            .callbackData("/back")
            .build()
}