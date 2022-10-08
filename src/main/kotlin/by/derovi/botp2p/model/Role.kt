package by.derovi.botp2p.model

enum class Role(val readableName: String, val description: String) {
    UNSUBSCRIBED("Без подписки", ""),
    STANDARD("Стандарт", """
        - Доступ к <b>/bundles</b>
        - Доступ к <b>/spot</b>
        - Доступ к <b>/tokens</b>
    """.trimIndent()),
    ADMIN("Админ", "");

    val isTariff: Boolean
        get() = this != UNSUBSCRIBED && this != ADMIN

    fun hasPermission(role: Role) = role.ordinal >= this.ordinal
}
