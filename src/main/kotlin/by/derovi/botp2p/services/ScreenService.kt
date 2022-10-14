package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.UserRepository
import me.ivmg.telegram.bot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class ScreenService {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var commandService: CommandService

    var isLastUpdate = false

    class ScreenData(val command: String, val args: Array<String>, val expiration: Long)

    val userIdToLastMessageId = mutableMapOf<Long, Int>()
    var userIdToScreenData = mutableMapOf<Long, ScreenData>()

    fun onUpdatableEdited(botUser: BotUser, messageId: Int, command: String, args: Array<String>) {
        if (botUser.isVirtual) {
            return
        }
        val lastMessageId = userIdToLastMessageId[botUser.id] ?: 0
        if (messageId < lastMessageId) return
        userIdToLastMessageId[botUser.id] = messageId
        userIdToScreenData[botUser.id] = ScreenData(command, args, System.currentTimeMillis() + 60 * 1000L)
    }

    fun onNonUpdatableEdited(botUser: BotUser, messageId: Int) {
        if (messageId == userIdToLastMessageId[botUser.id]) {
            userIdToScreenData.remove(botUser.id)
        }
    }

    fun onAnySent(botUser: BotUser) {
        update(botUser.id, true)
    }

    private fun update(userId: Long, last: Boolean = false) {
        val screenData = userIdToScreenData[userId] ?: return
        if (screenData.expiration < System.currentTimeMillis() || last) {
            isLastUpdate = true
            userIdToScreenData.remove(userId)
        }
        val botUser = userService.getBotUserById(userId, userIdToLastMessageId[userId])
        commandService.use(screenData.command, screenData.args, botUser, false)
        isLastUpdate = false
    }

//    @Scheduled(fixedDelay = 1000)
//    @Transactional
//    fun updateAll() {
//        for (userId in userRepository.getAllIds()) {
//            update(userId)
//        }
//    }
}
