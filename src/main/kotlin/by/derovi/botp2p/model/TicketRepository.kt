package by.derovi.botp2p.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TicketRepository : CrudRepository<Ticket, Long> {
    fun removeByUser(user: ServiceUser)
    fun getFirstByUser(user: ServiceUser): Ticket?
}