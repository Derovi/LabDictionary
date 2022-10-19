package by.derovi.botp2p.model

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SearchSettingsRepository : CrudRepository<SearchSettings, Long> {
}