package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.library.BundlesPreview
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.library.checkIfSelected
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.round

@Component
class PricesCommand : Command {
    @Autowired
    lateinit var bundleSearch: BundleSearch

    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    lateinit var spotService: SpotService

    fun Offer.usdtPrice() = this.price / spotService.price(this.token)

    override val name = "/prices"
    override val role = Role.STANDARD

    fun url(showFull: Boolean, buy: Boolean, taker: Boolean) =
        "/prices?$showFull&$buy&$taker"

    override fun use(user: BotUser, vararg args: String) {
        val showFull = args.firstOrNull()?.toBooleanStrictOrNull() ?: false
        val buy = args.getOrNull(1)?.toBooleanStrictOrNull() ?: true
        val taker = args.getOrNull(2)?.toBooleanStrictOrNull() ?: true

        val offers = bundlesService.searchBestPricesForUser(user.serviceUser, buy, taker)
        val currency = Currency.RUB
        val limit = if (showFull) 20 else 10

        user.sendMessage(buildString {
            append("<b>${if (buy) "Покупка" else "Продажа"}/${if (taker) "Тейкер" else "Мейкер"}, ${currency}</b>, " +
                    "<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>\n")

            append("Показана цена 1 usdt\n")
                for ((idx, offer) in offers.withIndex().take(limit)) {
                    append(
                        "<b>${idx + 1}.</b> " +
                            "${Utils.formatNumber(offer.usdtPrice())} руб. " +
                            "${Utils.createLink(offer.username, offer.link)} - " +
                            "<b>${offer.exchange?.name()}</b>, " +
                            "<b>${offer.paymentMethod}</b>, " +
                            "<b>${offer.token}</b> цена: <b>${offer.price} ${currency}</b>, " +
                            "[лимит ${offer.minLimit} - ${offer.maxLimit} ${currency}]" +
                            if (offer.completeCount == null) "" else
                                ", [успешно ${offer.completeCount}, ${offer.completeRate}%]")
                    append("  ${BundlesPreview.banLink(offer.username, offer.exchange!!.name())}\n")
                }
                toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("\uD83D\uDD04 Обновить")
                        .callbackData(url(showFull, buy, taker)).build(),
                    if (!showFull) {
                        InlineKeyboardButton.builder().text("\uD83D\uDE48 Больше")
                            .callbackData(url(true, buy, taker)).build()
                    } else {
                        InlineKeyboardButton.builder().text("\uD83E\uDDD0 Меньше")
                            .callbackData(url(false, buy, taker)).build()
                    }
                ))

                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("➡ Купить".checkIfSelected(buy, true))
                        .callbackData(url(showFull, true, taker)).build(),
                    InlineKeyboardButton
                        .builder()
                        .text("⬅️ Продать".checkIfSelected(buy, false))
                        .callbackData(url(showFull, false, taker)).build(),
                ))
                keyboardRow(listOf(
                    InlineKeyboardButton
                        .builder()
                        .text("⬇ Тейкер".checkIfSelected(taker, true))
                        .callbackData(url(showFull, buy, true)).build(),
                    InlineKeyboardButton
                        .builder()
                        .text("⬆ Мейкер".checkIfSelected(taker, false))
                        .callbackData(url(showFull, buy, false)).build(),
                ))

                keyboardRow(mutableListOf(InlineKeyboardButton
                    .builder()
                    .text("↩️ Главное меню")
                    .callbackData("/start")
                    .build()))
                build()
            }
        )
    }
}
