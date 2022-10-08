package by.derovi.botp2p.lang

class LangMap(val code: String, val baseMap: LangMap?, val translations: Map<String, String>) {
    constructor(code: String, translations: Map<String, String>) : this(code, null, translations)

    operator fun get(key: String): String {
        return translations[key] ?: (baseMap?.get(key)) ?: "null"
    }
}
