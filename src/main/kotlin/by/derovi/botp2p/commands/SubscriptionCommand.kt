package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.DialogService
import by.derovi.botp2p.services.PromoService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class SubscriptionCommand : Command {
    override val name = "/subscription"
    override val role = Role.UNSUBSCRIBED

    @Autowired
    lateinit var dialogService: DialogService

    @Autowired
    lateinit var promoService: PromoService

    @Autowired
    lateinit var buttonsService: ButtonsService

    override fun use(user: BotUser, vararg args: String) {
        val role = user.serviceUser.role

        user.sendMessage(
            with(StringBuilder()) {
                append(
                    when(role) {
                        Role.UNSUBSCRIBED -> "<b>У вас нет подписки!</b>\n"
                        Role.ADMIN -> "<b>Вы админ</b>\n"
                        else -> "Подписка <i>${role.readableName}</i> оплачена до " +
                                "<b>${Utils.formatDate(user.serviceUser.subscribedUntil)}</b>\n"
                    }
                )
                append("\n")
                val promo = user.serviceUser.promo
                if (promo != null) {
                    append("Ваша скидка: <b>${PromoService.percentToReadable(promo.discount)}</b>\n")
                }
                val referPromo = user.serviceUser.referPromo
                if (referPromo != null) {
                    append("Бонус от патрнеров: <b>${user.serviceUser.referBonus}$</b>\n")
                    append("Количество партрнеров: <b>${user.serviceUser.referNumber}</b>\n")
                    append("Ваш реферальный промокод: <code>${referPromo.id}</code>\n")
                    append("Ваша реферальная ссылка: ${Utils.createCommandLinkNoEncode("promo${referPromo.id}")}\n")

                    append("<i>Каждый, кто укажет этот промокод, получит скидку " +
                            "<b>${PromoService.percentToReadable(referPromo.discount)}</b>, " +
                            "а вы получите <b>${PromoService.percentToReadable(promoService.referReward)}</b> " +
                            "от его оплаты в качестве бонуса!</i>\n")
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                if (role.isTariff) {
                    keyboardRow(mutableListOf(
                        InlineKeyboardButton
                            .builder()
                            .text("Продлить")
                            .callbackData("/order?${role.name}")
                            .build()
                    ))
                }
                keyboardRow(mutableListOf(
                    InlineKeyboardButton
                        .builder()
                        .text(if (role.isTariff) "Изменить тариф" else "Выбрать тариф")
                        .callbackData("/tariffs")
                        .build()
                ))
                keyboardRow(mutableListOf(
                    InlineKeyboardButton
                        .builder()
                        .text("У меня есть промокод")
                        .callbackData("/promo")
                        .build()
                ))
                keyboardRow(mutableListOf(buttonsService.backButton()))
                build()
            }
        )
    }
}
