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
    override val role = Role.ADVANCED

    fun url(
        showFull: Boolean,
        buy: Boolean,
        taker: Boolean,
        exchange: Exchange?,
        token: Token?,
        command: String?
    ) = "/prices?$showFull&$buy&$taker&${exchange?.name()}&$token&${command}"

    override fun use(user: BotUser, vararg args: String) {
        val showFull = args.firstOrNull()?.toBooleanStrictOrNull() ?: false
        val buy = args.getOrNull(1)?.toBooleanStrictOrNull() ?: true
        val taker = args.getOrNull(2)?.toBooleanStrictOrNull() ?: true

        val chosenExchange = args.getOrNull(3)?.let { chosenExchange ->
            bundleSearch.commonExchanges.find { it.name().equals(chosenExchange, ignoreCase = true) }
        }
        val chosenToken = args.getOrNull(4)?.let { chosenToken ->
            Token.values().find { it.name.equals(chosenToken, ignoreCase = true) }
        }
        val command = args.getOrNull(5)

        val offers = bundlesService.searchBestPricesForUser(
            user.serviceUser,
            buy,
            taker,
            chosenExchange,
            chosenToken
        )
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
                when(command) {
                    "chooseexchange" -> {
                        bundleSearch.commonExchanges.asSequence().chunked(3).map {
                            it.map { exchange ->
                                InlineKeyboardButton.builder()
                                    .text(exchange.name())
                                    .callbackData(url(showFull, buy, taker, exchange, chosenToken, null))
                                    .build()
                            }
                        }.forEach(::keyboardRow)
                    }
                    "choosetoken" -> {
                        Token.values().asSequence().chunked(4).map {
                            it.map { token ->
                                InlineKeyboardButton.builder()
                                    .text(token.name)
                                    .callbackData(url(showFull, buy, taker, chosenExchange, token, null))
                                    .build()
                            }
                        }.forEach(::keyboardRow)
                    }
                    else -> {
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83D\uDD04 Обновить")
                                .callbackData(url(showFull, buy, taker, chosenExchange, chosenToken, null)).build(),
                            if (!showFull) {
                                InlineKeyboardButton.builder().text("\uD83D\uDE48 Больше")
                                    .callbackData(url(true, buy, taker, chosenExchange, chosenToken, null)).build()
                            } else {
                                InlineKeyboardButton.builder().text("\uD83E\uDDD0 Меньше")
                                    .callbackData(url(false, buy, taker, chosenExchange, chosenToken, null)).build()
                            }
                        ))

                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("➡ Купить".checkIfSelected(buy, true))
                                .callbackData(url(showFull, true, taker, chosenExchange, chosenToken, null)).build(),
                            InlineKeyboardButton
                                .builder()
                                .text("⬅️ Продать".checkIfSelected(buy, false))
                                .callbackData(url(showFull, false, taker, chosenExchange, chosenToken, null)).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("⬇ Тейкер".checkIfSelected(taker, true))
                                .callbackData(url(showFull, buy, true, chosenExchange, chosenToken, null)).build(),
                            InlineKeyboardButton
                                .builder()
                                .text("⬆ Мейкер".checkIfSelected(taker, false))
                                .callbackData(url(showFull, buy, false, chosenExchange, chosenToken, null)).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83D\uDCB1 Все биржи".checkIfSelected(chosenExchange == null))
                                .callbackData(url(showFull, buy, true, null, chosenToken, null)).build(),
                            InlineKeyboardButton
                                .builder()
                                .text(if (chosenExchange == null) " Выбрать биржу" else "✓ \uD83D\uDCB1 Выбрать [${chosenExchange.name()}]")
                                .callbackData(url(showFull, buy, false, chosenExchange, chosenToken, "chooseexchange")).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83E\uDE99 Все токены".checkIfSelected(chosenToken == null))
                                .callbackData(url(showFull, buy, true, chosenExchange, null, null)).build(),
                            InlineKeyboardButton
                                .builder()
                                .text(if (chosenToken == null) " Выбрать токен" else "✓ \uD83E\uDE99 Выбрать [${chosenToken.name}]")
                                .callbackData(url(showFull, buy, false, chosenExchange, chosenToken, "choosetoken")).build(),
                        ))
                    }
                }

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
