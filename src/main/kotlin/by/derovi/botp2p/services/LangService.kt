package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.lang.LangMap
import by.derovi.botp2p.services.langs.langEN
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class LangService {
    lateinit var langs: Map<String, LangMap>

    @Autowired
    lateinit var context: ApplicationContext

    @PostConstruct
    fun wireLangs() {
        langs = context.getBeansOfType(LangMap::class.java).values.associateBy { it.code }
    }

    private fun inquireLang(telegramLanguageCode: String) = langEN()

    fun getLangMap(languageCode: String?, telegramLanguageCode: String) =
        langs[languageCode ?: ""] ?: inquireLang(telegramLanguageCode)
}