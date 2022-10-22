package by.derovi.botp2p.library

import by.derovi.botp2p.services.CommandService
import io.seruco.encoding.base62.Base62
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

fun <T> String.checkIfSelected(selected: T, other: T) = if (selected == other) "✓ $this" else this
fun String.checkIfSelected(selected: Boolean) = if (selected) "✓ $this" else this

object Utils {
    val base62 = Base62.createInstance()

    fun normalizeSpread(value: Double) = round(value * 10000).toInt() / 100.0

    fun formatNumber(value: Double) = max(0.0, (value * 100).toInt() / 100.0)
    fun formatDate(millis: Long) = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Date(millis))

    fun createLink(text: String, link: String) = "<a href='$link'>$text</a>"

    fun createCommandLinkNoEncode(command: String)
            = "https://t.me/deroviBot?start=${command}"

    fun createCommandLink(text: String, command: String)
        = "<a href=\"https://t.me/deroviBot" +
            "?start=${String(base62.encode(command.toByteArray()))}\"" +
            ">$text</a>"

    fun drawTable(
        rowsTitles: List<String>,
        columnTitles: List<String>,
        numbers: List<List<Double>>,
        cutTitles: Boolean = true,
        wide: Int = 5
    ) = with(StringBuilder()) {
        fun formatNumber(number: Double): String {
            val str = String.format("%.5f", number).take(wide - 1)
            return if (str.last() == '.') {
                str.dropLast(1) + "% "
            } else {
                "$str%"
            }
        }

        val rowsNumber = min(4, rowsTitles.size)
        val columnsNUmber = min(4, columnTitles.size)

        fun drawRawSeparator() {
            for (idx2 in 0 .. columnsNUmber) {
                repeat(wide) { append("—") }
                if (idx2 < columnsNUmber) append("|")
            }
            append("\n")
        }

        repeat(wide) { append(" ") }
        append("|")

        fun cut(str: String) = if (cutTitles) str.take(wide) + " ".repeat(wide - min(wide, str.length)) else str
        for ((idx, title) in columnTitles.withIndex()) {
            if (idx >= columnsNUmber) {
                break
            }
            append(cut(title))
            if (idx < columnTitles.lastIndex) {
                append("|")
            }
        }
        append("\n")
        drawRawSeparator()
        for (idx1 in 0 until rowsNumber) {
            append(cut(rowsTitles[idx1]))
            append("|")
            for (idx2 in 0 until columnsNUmber) {
                append(formatNumber(numbers[idx1][idx2]))
                if (idx2 < columnsNUmber - 1) append("|")
            }
            append("\n")
            drawRawSeparator()
        }
        toString()
    }
}
