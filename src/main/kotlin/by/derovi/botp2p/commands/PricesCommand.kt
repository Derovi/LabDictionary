package by.derovi.botp2p.commands

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.exchange.*
import by.derovi.botp2p.library.BundlesPreview
import by.derovi.botp2p.library.Utils
import by.derovi.botp2p.library.checkIfSelected
import by.derovi.botp2p.model.Role
import by.derovi.botp2p.services.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

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
        exchanges: List<Exchange>?,
        tokens: List<Token>?,
        command: String?,
        pinnedBuyToken: Token?,
        pinnedBuyPrice: Double?,
        pinnedSellToken: Token?,
        pinnedSellPrice: Double?
    ) = context.getBean(CommandService::class.java).manageCommandUrl(
        "/prices?$showFull&$buy&$taker&${exchanges?.map(Exchange::name)?.joinToString(",")}&${tokens?.map(Token::name)?.joinToString(",")}&${command}&$pinnedBuyToken,$pinnedBuyPrice&$pinnedSellToken,$pinnedSellPrice")

//    fun url(
//        showFull: Boolean,
//        buy: Boolean,
//        taker: Boolean,
//        exchange: Exchange?,
//        token: Token?,
//        command: String?,
//        pinnedBuyToken: Token?,
//        pinnedBuyPrice: Double?,
//        pinnedSellToken: Token?,
//        pinnedSellPrice: Double?
//    ) = "/prices?$showFull&$buy&$taker&${exchange?.name()}&$token&${command}&$pinnedBuyToken&$pinnedBuyPrice&$pinnedSellToken&$pinnedSellPrice"


    override fun use(user: BotUser, vararg args: String) {
        val showFull = args.firstOrNull()?.toBooleanStrictOrNull() ?: false
        val buy = args.getOrNull(1)?.toBooleanStrictOrNull() ?: true
        val taker = args.getOrNull(2)?.toBooleanStrictOrNull() ?: true

        val chosenExchanges = args.getOrNull(3)?.split(",")?.map { exchange ->
            bundleSearch.commonExchanges.find { it.name().equals(exchange, ignoreCase = true) }
        }?.filterNotNull()?.ifEmpty { null }
        val chosenTokens = args.getOrNull(4)?.split(",")?.map { token ->
            Token.values().find { it.name.equals(token, ignoreCase = true) }
        }?.filterNotNull()?.ifEmpty { null }
        val command = args.getOrNull(5)
        fun parseParams(params: List<String>?): Pair<Token?, Double?> {
            if (params == null || params.size != 2) return null to null
            val token = Token.values().find { it.name == params.first() }
            val price = params.get(1).toDoubleOrNull()
            if (token == null || price == null) {
                return null to null
            }
            return token to price
        }

        val (pinnedBuyToken, pinnedBuyPrice) = parseParams(args.getOrNull(6)?.split(","))
        val (pinnedSellToken, pinnedSellPrice) = parseParams(args.getOrNull(7)?.split(","))

        val offers = bundlesService.searchBestPricesForUser(
            user.serviceUser,
            buy,
            taker,
            chosenExchanges,
            chosenTokens
        )

        val currency = Currency.RUB
        val limit = if (showFull) 20 else 10

        user.sendMessage(buildString {
            append("<b>${if (buy) "Покупка" else "Продажа"}/${if (taker) "Тейкер" else "Мейкер"}, ${currency}</b>, " +
                    "<i>${Utils.formatDate(bundlesService.lastUpdateTime)}</i>\n")

            if (!buy && pinnedBuyToken != null && pinnedBuyPrice != null) {
                append("\uD83D\uDCCC Закреплена покупка <b>${pinnedBuyToken.name}</b> " +
                        "за <b>${Utils.formatNumber(pinnedBuyPrice)} ${currency.name}</b> " +
                        Utils.createCommandLink(" ❌ ",
                        url(
                            showFull,
                            false,
                            taker,
                            chosenExchanges,
                            chosenTokens,
                            null,
                            null,
                            null,
                            pinnedSellToken,
                            pinnedSellPrice
                        )) +
                        "\n")
            }
            if (buy && pinnedSellToken != null && pinnedSellPrice != null) {
                append("\uD83D\uDCCC Закреплена продажа <b>${pinnedSellToken.name}</b> " +
                        "за <b>${Utils.formatNumber(pinnedSellPrice)} ${currency.name}</b> " +
                        Utils.createCommandLink(" ❌ ",
                            url(
                                showFull,
                                false,
                                taker,
                                chosenExchanges,
                                chosenTokens,
                                null,
                                pinnedBuyToken,
                                pinnedBuyPrice,
                                null,
                                null
                            )) +
                        "\n")
            }

            append("Показана цена 1 usdt\n")

            fun appendForOffer(offer: Offer) {
                append("${Utils.formatNumber(offer.usdtPrice())} руб. ")

                fun appendSpread(
                    buyToken: Token,
                    buyPrice: Double,
                    sellToken: Token,
                    sellPrice: Double
                ) = append("[")
                    .append(Utils.normalizeSpread((sellPrice / spotService.price(sellToken)) / (buyPrice / spotService.price(buyToken)) - 1))
                    .append("%] ")

                if (buy && pinnedSellToken != null && pinnedSellPrice != null) {
                    appendSpread(offer.token, offer.price, pinnedSellToken, pinnedSellPrice)
                } else if (!buy && pinnedBuyToken != null && pinnedBuyPrice != null) {
                    appendSpread(pinnedBuyToken, pinnedBuyPrice, offer.token, offer.price)
                }

                append(
                    "${Utils.createLink(offer.username, offer.link)} - " +
                            "<b>${offer.exchange?.name()}</b>, " +
                            "<b>${offer.paymentMethod}</b>, " +
                            "<b>${offer.token}</b> цена: <b>${offer.price} ${currency}</b>, " +
                            "[лимит ${offer.minLimit} - ${offer.maxLimit} ${currency}]" +
                            if (offer.completeCount == null) "" else
                                ", [успешно ${offer.completeCount}, ${offer.completeRate}%] ")
                if (buy) {
                    append(
                        Utils.createCommandLink(
                            " \uD83D\uDCCC ",
                            url(
                                showFull,
                                false,
                                taker,
                                chosenExchanges,
                                chosenTokens,
                                null,
                                offer.token,
                                offer.price,
                                pinnedSellToken,
                                pinnedSellPrice
                            )
                        )
                    )
                } else {
                    append(
                        Utils.createCommandLink(
                            " \uD83D\uDCCC ",
                            url(
                                showFull,
                                true,
                                taker,
                                chosenExchanges,
                                chosenTokens,
                                null,
                                pinnedBuyToken,
                                pinnedBuyPrice,
                                offer.token,
                                offer.price
                            )
                        )
                    )
                }

                append("  ${BundlesPreview.banLink(offer.username, offer.exchange!!.name())}\n")
            }
            if (taker) {
                for ((idx, offer) in offers.withIndex().take(limit)) {
                    append("<b>${idx + 1}.</b> ")
                    appendForOffer(offer)
                }
            } else {
                val groupedOffers = offers.groupBy { it.exchange to it.token }.entries.sortedWith { first, second ->
                    val firstPrice = first.value.firstOrNull()?.usdtPrice()
                    val secondPrice = second.value.firstOrNull()?.usdtPrice()

                    val result = firstPrice?.compareTo(secondPrice ?: Double.POSITIVE_INFINITY) ?: 1
                    return@sortedWith result * if (buy) 1 else -1
                }.map { it.value.take(4) }.take(10)

                var idx = 0
                for ((groupIdx, group) in groupedOffers.withIndex()) {
                    for ((offerIdx, offer) in group.withIndex()) {
                        if (++idx > limit) break
                        if (offerIdx == 0) {
                            append("<b>${groupIdx + 1}</b>. ")
                        } else {
                            append("    ")
                        }
                        appendForOffer(offer)
                    }
                    append("\n")
                }
            }
            toString()
            },
            with(InlineKeyboardMarkup.builder()) {
                val cancelButton = InlineKeyboardButton.builder()
                    .text("↩️ Выбрать")
                    .callbackData(url(
                        showFull,
                        buy,
                        taker,
                        chosenExchanges,
                        chosenTokens,
                        null,
                        pinnedBuyToken,
                        pinnedBuyPrice,
                        pinnedSellToken,
                        pinnedSellPrice
                    )).build()
                when(command) {
                    "chooseexchange" -> {
                        val exchanges = chosenExchanges ?: listOf()
                        bundleSearch.commonExchanges.asSequence().chunked(3).map {
                            it.map { exchange ->
                                val selected = exchange in exchanges
                                InlineKeyboardButton.builder()
                                    .text(exchange.name().checkIfSelected(selected))
                                    .callbackData(url(
                                        showFull,
                                        buy,
                                        taker,
                                        exchanges.toMutableList().apply {
                                            if (selected) {
                                                remove(exchange)
                                            } else {
                                                add(exchange)
                                            }
                                        },
                                        chosenTokens,
                                        "chooseexchange",
                                        pinnedBuyToken,
                                        pinnedBuyPrice,
                                        pinnedSellToken,
                                        pinnedSellPrice
                                    )).build()
                            }
                        }.forEach(::keyboardRow)
                        keyboardRow(mutableListOf(cancelButton))
                    }
                    "choosetoken" -> {
                        val tokens = chosenTokens ?: listOf()
                        Token.values().asSequence().chunked(4).map {
                            it.map { token ->
                                val selected = token in tokens
                                InlineKeyboardButton.builder()
                                    .text(token.name.checkIfSelected(selected))
                                    .callbackData(url(
                                        showFull,
                                        buy,
                                        taker,
                                        chosenExchanges,
                                        tokens.toMutableList().apply {
                                            if (selected) {
                                                remove(token)
                                            } else {
                                                add(token)
                                            }
                                         },
                                        "choosetoken",
                                        pinnedBuyToken,
                                        pinnedBuyPrice,
                                        pinnedSellToken,
                                        pinnedSellPrice
                                    )).build()
                            }
                        }.forEach(::keyboardRow)
                        keyboardRow(mutableListOf(cancelButton))
                    }
                    else -> {
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83D\uDD04 Обновить")
                                .callbackData(url(
                                    showFull,
                                    buy,
                                    taker,
                                    chosenExchanges,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                            if (!showFull) {
                                InlineKeyboardButton.builder().text("\uD83D\uDE48 Больше")
                                    .callbackData(url(
                                        true,
                                        buy,
                                        taker,
                                        chosenExchanges,
                                        chosenTokens,
                                        null,
                                        pinnedBuyToken,
                                        pinnedBuyPrice,
                                        pinnedSellToken,
                                        pinnedSellPrice
                                    )).build()
                            } else {
                                InlineKeyboardButton.builder().text("\uD83E\uDDD0 Меньше")
                                    .callbackData(url(
                                        false,
                                        buy,
                                        taker,
                                        chosenExchanges,
                                        chosenTokens,
                                        null,
                                        pinnedBuyToken,
                                        pinnedBuyPrice,
                                        pinnedSellToken,
                                        pinnedSellPrice
                                    )).build()
                            }
                        ))

                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("➡ Купить".checkIfSelected(buy, true))
                                .callbackData(url(
                                    showFull,
                                    true,
                                    taker,
                                    chosenExchanges,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                            InlineKeyboardButton
                                .builder()
                                .text("⬅️ Продать".checkIfSelected(buy, false))
                                .callbackData(url(showFull,
                                    false,
                                    taker,
                                    chosenExchanges,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("⬇ Тейкер".checkIfSelected(taker, true))
                                .callbackData(url(showFull,
                                    buy,
                                    true,
                                    chosenExchanges,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                            InlineKeyboardButton
                                .builder()
                                .text("⬆ Мейкер".checkIfSelected(taker, false))
                                .callbackData(url(showFull,
                                    buy,
                                    false,
                                    chosenExchanges,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83D\uDCB1 Все биржи из ⚙".checkIfSelected(chosenExchanges == null))
                                .callbackData(url(
                                    showFull,
                                    buy,
                                    taker,
                                    null,
                                    chosenTokens,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                            InlineKeyboardButton
                                .builder()
                                .text(if (chosenExchanges == null) " Выбрать биржу" else "✓ \uD83D\uDCB1 Выбрать [${chosenExchanges.first().name()}" +
                                        (if (chosenExchanges.size > 1) "+${chosenExchanges.size - 1}" else "")
                                        + "]")
                                .callbackData(url(
                                    showFull,
                                    buy,
                                    taker,
                                    chosenExchanges,
                                    chosenTokens,
                                    "chooseexchange",
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                        ))
                        keyboardRow(listOf(
                            InlineKeyboardButton
                                .builder()
                                .text("\uD83E\uDE99 Все токены из ⚙".checkIfSelected(chosenTokens == null))
                                .callbackData(url(
                                    showFull,
                                    buy,
                                    taker,
                                    chosenExchanges,
                                    null,
                                    null,
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                            InlineKeyboardButton
                                .builder()
                                .text(if (chosenTokens == null) " Выбрать токен" else "✓ \uD83E\uDE99 Выбрать [${chosenTokens.first().name}" +
                                        (if (chosenTokens.size > 1) "+${chosenTokens.size - 1}" else "")
                                        + "]")
                                .callbackData(url(
                                    showFull,
                                    buy,
                                    taker,
                                    chosenExchanges,
                                    chosenTokens,
                                    "choosetoken",
                                    pinnedBuyToken,
                                    pinnedBuyPrice,
                                    pinnedSellToken,
                                    pinnedSellPrice
                                )).build(),
                        ))
                        keyboardRow(mutableListOf(InlineKeyboardButton
                            .builder()
                            .text("↩️ Главное меню")
                            .callbackData("/start")
                            .build()))
                    }
                }
                build()
            }
        )
    }
}
