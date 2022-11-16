package by.derovi.botp2p.library

import by.derovi.botp2p.exchange.BundleSearchResult
import by.derovi.botp2p.exchange.Offer
import by.derovi.botp2p.exchange.OrderType
import by.derovi.botp2p.services.FeesService

object BundlesPreview {
    fun fullView(bundle: BundleSearchResult) = with(StringBuilder()) {
        append(spreadAndFee(bundle))
        append("\n")
        append(buy(bundle))
        append(way(bundle))
        append(sell(bundle))
        append("\n")
        append(offers(bundle, 5))
        append("\n")
        append(table(bundle))
        append("\n")
        append(warning(bundle))
        toString()
    }

    val link = Utils.createLink("хеджировать", "http://ovi.by/bot/#%D1%85%D0%B5%D0%B4%D0%B6%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5")

    fun warning(bundle: BundleSearchResult) = buildString {
        val hedgeBuy = !bundle.buyToken.isStable()
        val hedgeSell = !bundle.sellToken.isStable() && bundle.sellOffers.first().orderType == OrderType.BUY
        if (hedgeBuy || hedgeSell) {
            append("<i>  Рекомендуем </i>$link ")
            if (hedgeBuy) append("<i>покупку</i>")
            if (hedgeBuy && hedgeSell) append("<i> и </i>")
            if (hedgeSell) append("<i>продажу</i>")
            append("<i>!</i>\n")
        }
    }

    fun preview(bundle: BundleSearchResult) = with(StringBuilder()) {
        append(spreadAndFee(bundle))
        append("\n")
        append(buy(bundle))
        append(sell(bundle))
        append("\n")
        append(offers(bundle, 4))
        toString()
    }

    private fun spreadAndFee(bundle: BundleSearchResult) = buildString {
        val worstSpread = bundle.spreadsWithFee.minOf { row -> row.minOf { number -> number } }
        val fee = Utils.normalizeSpread(bundle.transferGuide.calculateFinalFee())
        append("\uD83D\uDCC8 Спред: <b>${if (worstSpread < 1e-7) "до " else "${Utils.normalizeSpread(worstSpread)}%-"}${Utils.normalizeSpread(bundle.bestSpread)}%</b>\n")
        if (fee > 0) {
            append("✡️ Комиссия: <b>${fee}%</b>\n")
        }
        toString()
    }

    private fun way(bundle: BundleSearchResult) = buildString {
        if (bundle.transferGuide.steps.find { it is FeesService.TransferStep.Change } != null) {
            bundle.transferGuide.steps.forEach {
                when (it) {
                    is FeesService.TransferStep.Change -> append("<i>  Обменять --> ${it.to.readableName}</i>\n")
                    is FeesService.TransferStep.Transfer -> append("<i>  Перевести --> ${it.to.name()}</i>\n")
                }
            }
        }
    }

    private fun buy(bundle: BundleSearchResult) = buildString {
        append("↘️ купить <b>${bundle.buyToken}</b> на <b>${bundle.buyExchange.name()}</b>")
        if (bundle.buyOffers.first().orderType == OrderType.SELL) {
            append(" <i>мейкер</i>")
        } else {
            append(" <i>тейкер</i>")
        }
        append("\n")
        toString()
    }

    private fun sell(bundle: BundleSearchResult) = buildString {
        append("↙️ продать <b>${bundle.sellToken}</b> на <b>${bundle.sellExchange.name()}</b>")
        if (bundle.sellOffers.first().orderType == OrderType.BUY) {
            append(" <i>мейкер</i>")
        }else {
            append(" <i>тейкер</i>")
        }
        append("\n")
        toString()
    }

    fun banLink(name: String, exchange: String) = Utils.createCommandLink(" \uD83D\uDEAB ", "/ban?$name&$exchange")
    fun textForOffer(bundle: BundleSearchResult, idx: Int, offer: Offer) =
        "<b>[${idx + 1}]</b> " +
                "${Utils.createLink(offer.username, offer.link)} - <b>${offer.paymentMethod}</b>, " +
                "<b>${offer.token}</b> цена: <b>${offer.price} ${bundle.currency}</b>, " +
                "[лимит ${offer.minLimit} - ${offer.maxLimit} ${bundle.currency}]" +
                if (offer.completeCount == null) "" else
                ", [успешно ${offer.completeCount}, ${offer.completeRate}%]"

    private fun offers(bundle: BundleSearchResult, nOffers: Int) = buildString {
        append("<b>Купить:</b>\n")
        for ((idx, offer) in bundle.buyOffers.take(nOffers).withIndex()) {
            append(textForOffer(bundle, idx, offer))
            if (offer.orderType == OrderType.SELL) {
                append(" <i>мейкер</i>")
            }else {
                append(" <i>тейкер</i>")
            }
            append("  ${banLink(offer.username, offer.exchange!!.name())}")
            append("\n")
        }
        append("\n<b>Продать:</b>\n")
        for ((idx, offer) in bundle.sellOffers.take(nOffers).withIndex()) {
            append(textForOffer(bundle, idx, offer))
            if (offer.orderType == OrderType.BUY) {
                append(" <i>мейкер</i>")
            }else {
                append(" <i>тейкер</i>")
            }
            append("  ${banLink(offer.username, offer.exchange!!.name())}")
            append("\n")
        }
        toString()
    }

    private fun table(bundle: BundleSearchResult) = buildString {
        append("<code>")
        append(Utils.drawTable(
            bundle.buyOffers.map { it.username },
            bundle.sellOffers.map { it.username },
            bundle.spreadsWithFee.map { it.map { number -> number * 100 } },
            cutTitles = true
        ))
        append("</code>")
        toString()
    }
}