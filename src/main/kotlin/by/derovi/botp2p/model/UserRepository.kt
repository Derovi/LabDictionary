package by.derovi.botp2p.model

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CrudRepository<ServiceUser, Long> {
    @Query("select p.id from #{#entityName} p")
    fun getAllIds(): List<Long>
}