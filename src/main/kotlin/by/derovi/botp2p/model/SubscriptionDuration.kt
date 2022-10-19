package by.derovi.botp2p.model

enum class SubscriptionDuration(val days: Int, val readableName: String) {
    WEEK(7, "Неделя") {
        override val readableName2: String
            get() = "Неделю"
    },
    MONTH(30, "Месяц"),
    SEASON(90, "3 Месяца"),
    YEAR(365, "Год");

    open val readableName2
        get() = readableName
}