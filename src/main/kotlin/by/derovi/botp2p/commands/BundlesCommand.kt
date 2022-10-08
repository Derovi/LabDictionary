package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.Offer
import by.derovi.botp2p.exchange.OrderType
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.model.CurrencyAndPaymentMethod
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.BundlesService
import by.derovi.botp2p.services.ButtonsService
import by.derovi.botp2p.services.CommandService
import by.derovi.botp2p.services.FeesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.round

@Component
class BundlesCommand : Command {
    @Autowired
    lateinit var bundlesService: BundlesService

    @Autowired
    lateinit var buttonsService: ButtonsService

    @Autowired
    lateinit var context: ApplicationContext

    override val name = "/bundles"
    override val role = Role.STANDARD

    override fun use(user: BotUser, vararg args: String) {
        val commandService = context.getBean(CommandService::class.java)

        val ttOnly = args.getOrNull(1)?.toBooleanStrictOrNull() ?: false
        val bundles = if (ttOnly) bundlesService.userToBundleSearchResulTT[user.id] else bundlesService.userToBundleSearchResult[user.id]

        if (bundles == null || bundles.isEmpty()) {
            user.sendMessage("<b>Связок нет! Попробуйте позже</b>")
            commandService.back(user)
            return
        }

        val bundleIdx = args.firstOrNull()?.toIntOrNull().takeIf { it in bundles.indices } ?: 0

        fun banLink(name: String, exchange: String) = Utils.createCommandLink("[ban]", "/ban?$name&$exchange")

        val bundle = bundles[bundleIdx]
        val worstSpread = bundle.spreadsWithFee.minOf { row -> row.minOf { number -> number } }
        val text = with(StringBuilder()) {
            val fee = Utils.normalizeSpread(bundle.transferGuide.calculateFinalFee())
            append("<b>${bundleIdx + 1}/${bundles.size}, <i>${bundle.currency}</i></b>, " +
                    "<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>\n")
            append("Spread: <b>${if (worstSpread < 1e-7) "up to " else "${Utils.normalizeSpread(worstSpread)}%-"}${Utils.normalizeSpread(bundle.bestSpread)}%</b>\n")
            append("Fee: <b>${fee}%</b>\n")
            append("\n")
            append("buy <b>${bundle.buyToken}</b> on <b>${bundle.buyExchange.name()}</b>")
            if (bundle.buyOffers.first().orderType == OrderType.SELL) {
                append(" <i>maker</i>")
            }
            append("\n")
            if (bundle.transferGuide.steps.find { it is FeesService.TransferStep.Change } != null) {
                bundle.transferGuide.steps.forEach {
                    when (it) {
                        is FeesService.TransferStep.Change -> append("<i>Change --> ${it.to.readableName}</i>\n")
                        is FeesService.TransferStep.Transfer -> append("<i>Transfer --> ${it.to.name()}</i>\n")
                    }
                }
            }
            append("sell <b>${bundle.sellToken}</b> on <b>${bundle.sellExchange.name()}</b>")
            if (bundle.sellOffers.first().orderType == OrderType.BUY) {
                append(" <i>maker</i>")
            }
            append("\n\n")
            append("<b><i>Best buy:</i></b>\n")

            fun textForOffer(idx: Int, offer: Offer) =
                "<b>[${idx + 1}]</b> " +
                "${Utils.createLink(offer.username, offer.link)} - <b>${offer.paymentMethod}</b>, " +
                "<b>${offer.token}</b> price: <b>${offer.price} ${bundle.currency}</b>, " +
                "[limit ${offer.minLimit} - ${offer.maxLimit} ${bundle.currency}], " +
                "[success ${offer.completeCount}, ${offer.completeRate}%]"

            for ((idx, offer) in bundle.buyOffers.withIndex()) {
                append(textForOffer(idx, offer))
                if (offer.orderType == OrderType.SELL) {
                    append(" <i>maker</i>")
                }
                append("  ${banLink(offer.username, offer.exchange!!.name())}")
                append("\n")
            }
            append("\n<b><i>Best sell:</i></b>\n")
            for ((idx, offer) in bundle.sellOffers.withIndex()) {
                append(textForOffer(idx, offer))
                if (offer.orderType == OrderType.BUY) {
                    append(" <i>maker</i>")
                }
                append("  ${banLink(offer.username, offer.exchange!!.name())}")
                append("\n")
            }

            append("\n<code>")
            append(Utils.drawTable(
                bundle.buyOffers.map { it.username },
                bundle.sellOffers.map { it.username },
                bundle.spreadsWithFee.map { it.map { number -> number * 100 } },
                cutTitles = true
            ))
            append("</code>")
            toString()
        }
        val columns = mutableListOf<InlineKeyboardButton>()
        if (bundleIdx > 0) columns.add(
            InlineKeyboardButton.builder().text("Предыдущая").callbackData("/bundles?${bundleIdx - 1}").build()
        )
        columns.add(InlineKeyboardButton.builder().text("Обновить")

            .callbackData(commandService.lastCommand(user)).build())
        if (bundleIdx + 1 < bundles.size) {
            columns.add(
                InlineKeyboardButton.builder().text("Следующая").callbackData("/bundles?${bundleIdx + 1}").build()
            )
        }

        user.sendMessage(
            text,
            with(InlineKeyboardMarkup.builder()) {
                keyboardRow(columns)

                val availableCurrencies = user.serviceUser.userSettings.paymentMethods
                    .map(CurrencyAndPaymentMethod::currency)
                    .distinct()

                val chosenCurrency = user.serviceUser.userSettings.chosenCurrency
                if (availableCurrencies.size > 1 || chosenCurrency != availableCurrencies.first()) {
                    keyboardRow(with(mutableListOf<InlineKeyboardButton>()) {
                        add(
                            InlineKeyboardButton
                                .builder()
                                .text("Все" + if (chosenCurrency == null) " ✓" else "")
                                .callbackData("/chooseCurrency")
                                .build()
                        )

                        for (currency in availableCurrencies) {
                            add(
                                InlineKeyboardButton
                                    .builder()
                                    .text(currency.name + if (chosenCurrency == currency) " ✓" else "")
                                    .callbackData("/chooseCurrency?${currency.name}")
                                    .build()
                            )
                        }
                        this
                    })
                }
                keyboardRow(mutableListOf(buttonsService.modeButton(user.serviceUser.userSettings)))
                keyboardRow(mutableListOf(InlineKeyboardButton.builder().text("Главное меню").callbackData("/start").build()))
                build()
            }
        )
    }
}
