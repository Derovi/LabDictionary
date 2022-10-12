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
        var text = with(StringBuilder()) {
            append("<b>====================== Меню =====================</b>\n")
            append("<b>/bundles</b> - Получить список связок\n")
            append("<b>/settings</b> - Изменить настройки\n")
            append("<b>/guide</b> - Получить информацию\n")
            append("<b>/subscription</b> - Информация о вашей подписке\n")
            toString()
        }

        val keyboardBuilder = InlineKeyboardMarkup.builder().keyboardRow(mutableListOf(
            InlineKeyboardButton.builder().text("\uD83D\uDCC9 Связки").callbackData("/bundles").build(),
            InlineKeyboardButton.builder().text("⚙️ Настройки").callbackData("/settings").build(),
            InlineKeyboardButton.builder().text("\uD83D\uDCD6 Инструкция").url("https://telegra.ph/Tarify-10-10").build(),
            InlineKeyboardButton.builder().text("\uD83D\uDC49 Подписка").callbackData("/subscription").build(),
        ))

        if (user.isAdmin) {
            keyboardBuilder.keyboardRow(mutableListOf(
                InlineKeyboardButton.builder().text("Пользователи").callbackData("/users").build(),
                InlineKeyboardButton.builder().text("Найти по id").callbackData("/user").build(),
                InlineKeyboardButton.builder().text("Подписать").callbackData("/subscribe").build(),
            ))
        }

        user.sendMessage(text, keyboardBuilder.build())
    }
}
