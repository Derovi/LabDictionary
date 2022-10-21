package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class StartCommand : Command {
    override val name = "/start"
    override val role = Role.UNSUBSCRIBED

    override fun use(user: BotUser, vararg args: String) {
        user.sendMessage(
            buildString {
                append("<b>\uD83E\uDD16 DEROVI p2p BOT</b> — ")
                append("Твой помощник в поиске самых лучших связок и больших спредов\n")
                append("\n")
                append("Краткий обзор: https://www.youtube.com/watch?v=dQw4w9WgXcQ\n")
                append("Полная инструкция: https://www.youtube.com/watch?v=dQw4w9WgXcQ\n")
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton.builder().text("\uD83D\uDCC9 Связки").callbackData("/bundles").build(),
                    InlineKeyboardButton.builder().text("\uD83E\uDD47 Лучшие цены").callbackData("/prices").build(),
                ))
                keyboardRow(listOf(
                    InlineKeyboardButton.builder().text("⚙️ Настройки").callbackData("/settings").build(),
                    InlineKeyboardButton.builder().text("\uD83D\uDCD6 Инструкция").url("https://telegra.ph/Tarify-10-10").build(),
                    InlineKeyboardButton.builder().text("\uD83D\uDC49 Подписка").callbackData("/subscription").build(),
                ))
                if (user.isAdmin) {
                    keyboardRow(listOf(
                        InlineKeyboardButton.builder().text("Пользователи").callbackData("/users").build(),
                        InlineKeyboardButton.builder().text("Тикеты").callbackData("/tickets").build(),
                        InlineKeyboardButton.builder().text("Подписать").callbackData("/subscribe").build(),
                    ))
                }
                build()
            }
        )
    }
}
