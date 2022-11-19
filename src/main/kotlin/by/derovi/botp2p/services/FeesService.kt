package by.derovi.botp2p.services

import by.derovi.botp2p.exchange.Exchange
import by.derovi.botp2p.exchange.Token
import by.derovi.botp2p.exchange.exchanges.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.math.max

class Fee(val percentFee: Double, val fixedFee: Double) {
    operator fun plus(other: Fee) = Fee(percentFee + other.percentFee, fixedFee + other.fixedFee)
}

val noFee = Fee(0.0, 0.0)
fun percentFee(percent: Double) = Fee(percent, 0.0)
fun fixedFee(amount: Double) = Fee(0.0, amount)

val spotFees = mapOf(
    Huobi to Token.values().associateWith { percentFee(0.2) },
    Binance to Token.values().associateWith { percentFee(0.1) },
    Bybit to Token.values().associateWith { percentFee(0.1) },
    OKX to Token.values().associateWith { percentFee(0.15) },
    Kucoin to Token.values().associateWith { percentFee(0.1) },
    Bitzlato to Token.values().associateWith { percentFee(0.2) },
)

val withdrawFees = mapOf(
    Huobi to mapOf(
        Token.USDT to fixedFee(0.0),
//        Token.BTC to fixedFee(0.0004)
    ),
    Binance to mapOf(

    )
)

fun applyFee(value: Double, fee: Fee): Double = (value - fee.fixedFee) * (1 - fee.percentFee / 100.0)

@Service
class FeesService {
    @Autowired
    lateinit var spotService: SpotService

    sealed class TransferStep {
        class Transfer(val to: Exchange) : TransferStep()
        class Change(val to: Token): TransferStep()
    }

    inner class TransferGuide(val startToken: Token, val startExchange: Exchange, val startValue: Double) {
        private var currentToken = startToken
        private var currentExchange = startExchange
        private var currentValue = startValue

        val steps = mutableListOf<TransferStep>()

        fun calculateFinalFee(): Double {
            return max(
                0.0,
                1 - currentValue * spotService.price(currentToken) / startValue / spotService.price(startToken)
            )
        }

        fun add(step: TransferStep): TransferGuide {
            when(step) {
                is TransferStep.Transfer -> {
                    if (currentExchange == step.to) {
                        return this
                    }
                    steps.add(step)
                    currentValue = applyFee(currentValue, withdrawFees[currentExchange]?.get(currentToken) ?: noFee)
                    currentExchange = step.to
                }
                is TransferStep.Change -> {
                    if (currentToken == step.to) {
                        return this
                    }
                    val feeToken = if (currentToken == Token.USDT) {
                        currentValue /= spotService.price(step.to)
                        step.to
                    } else if (step.to == Token.USDT) {
                        currentValue *= spotService.price(currentToken)
                        currentToken
                    } else {
                        throw IllegalArgumentException("Can't change $currentValue and ${step.to}")
                    }
                    steps.add(step)
                    currentValue = applyFee(currentValue, spotFees[currentExchange]?.get(feeToken) ?: noFee)
                    currentToken = step.to
                }
                else -> noFee
            }
            return this
        }
    }

    fun findTransferGuide(
        exchange1: Exchange,
        exchange2: Exchange,
        token1: Token,
        token2: Token,
        value: Double
    ): TransferGuide {
        if (exchange1 == exchange2 && token1 == token2) {
            return TransferGuide(token1, exchange1, value)
        }
        if (exchange1 == exchange2 && token1 != token2) {
            return TransferGuide(token1, exchange1, value)
                .add(TransferStep.Change(Token.USDT))
                .add(TransferStep.Change(token2))
        }
        if (exchange1 != exchange2 && token1 == token2) {
            return TransferGuide(token1, exchange1, value)
                .add(TransferStep.Transfer(exchange2))
        }

        val transferGuides = listOf(
            TransferGuide(token1, exchange1, value)
                .add(TransferStep.Change(Token.USDT))
                .add(TransferStep.Transfer(exchange2))
                .add(TransferStep.Change(token2))
        )
        return transferGuides.minByOrNull { it.calculateFinalFee() }!!
    }
}