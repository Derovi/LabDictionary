package by.derovi.botp2p.services

import by.derovi.botp2p.model.*

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class TicketsService {
    @Autowired
    lateinit var ticketRepository: TicketRepository

    @Autowired
    lateinit var subscriptionService: SubscriptionService

    final val addresses = listOf(
        "a",
        "b",
        "c",
        "d"
    )

    var lastUsedAddressId = Random.nextInt(addresses.size)

    fun getAddress(): String {
        lastUsedAddressId = (lastUsedAddressId + 1) % addresses.size
        return addresses[lastUsedAddressId]
    }

    fun getAllTickets() = ticketRepository.findAll()
    fun getTicket(user: ServiceUser) = ticketRepository.getFirstByUser(user)

    fun approveByUser(ticket: Ticket) {
        ticket.approvedByUserAt = System.currentTimeMillis()
        ticketRepository.save(ticket)
    }

    fun cancelByUser(ticket: Ticket) = cancelByAdmin(ticket)

    fun approveByAdmin(ticket: Ticket) {
        subscriptionService.subscribe(ticket.user.userId, ticket.role, ticket.duration.days)
        ticketRepository.delete(ticket)
    }

    fun cancelByAdmin(ticket: Ticket) {
        ticketRepository.delete(ticket)
    }

    fun createTicket(
        user: ServiceUser,
        price: Long,
        promoPrice: Long,
        role: Role,
        subscriptionDuration: SubscriptionDuration
    ): Ticket {
        ticketRepository.removeByUser(user)
        ticketRepository
        val ticket = Ticket(
            0,
            user,
            price,
            promoPrice,
            role,
            subscriptionDuration,
            getAddress(),
            System.currentTimeMillis(),
            null
        )
        ticketRepository.save(ticket)
        return ticket
    }
}
