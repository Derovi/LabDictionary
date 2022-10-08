package by.derovi.botp2p.services.langs

import by.derovi.botp2p.lang.LangMap
import org.springframework.context.annotation.Bean

@Bean
fun langRU() = LangMap("ru", langEN(),  mapOf(
    "" to ""
))
