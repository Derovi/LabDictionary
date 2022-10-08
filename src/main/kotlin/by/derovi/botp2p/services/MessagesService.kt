package by.derovi.botp2p.services

import by.derovi.botp2p.BotUser
import by.derovi.botp2p.model.Role
import org.springframework.stereotype.Service

@Service
class MessagesService {
    fun update(botUser: BotUser) {
        if (botUser.serviceUser.subscribedUntil >= System.currentTimeMillis()) {
            botUser.serviceUser.role = Role.UNSUBSCRIBED
        }
    }
}