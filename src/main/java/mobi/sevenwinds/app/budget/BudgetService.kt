package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val author = findAuthorOrFail(body.authorId)

            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = author
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val allRows = BudgetTable.select { BudgetTable.year eq param.year }
            val allEntities = BudgetEntity.wrapRows(allRows).toList()

            val filteredEntities = filterEntitiesByParams(allEntities, param)
            val sumByType = calculateStatsByType(filteredEntities)
            val paginatedEntities = paginateAndSortEntities(filteredEntities, param)

            return@transaction BudgetYearStatsResponse(
                total = filteredEntities.size,
                totalByType = sumByType,
                items = paginatedEntities
            )
        }
    }

    private fun findAuthorOrFail(authorId: Int?): AuthorEntity? {
        return authorId?.let {
            AuthorEntity.findById(it) ?: error("Автор с id=$it не найден")
        }
    }

    private fun filterEntitiesByParams(entities: List<BudgetEntity>, param: BudgetYearParam): List<BudgetEntity> {
        return entities.filter { entity ->
            val matchesAuthorId = param.authorId?.let { it == entity.author?.id?.value } ?: true
            val matchesFio = param.fio?.let { fragment ->
                entity.author?.fio?.contains(fragment, ignoreCase = true)
            } ?: true

            matchesAuthorId && matchesFio
        }
    }

    private fun calculateStatsByType(entities: List<BudgetEntity>): Map<String, Int> {
        return entities.groupBy { it.type.name }
            .mapValues { it.value.sumOf { it.amount } }
    }

    private fun paginateAndSortEntities(
        entities: List<BudgetEntity>,
        param: BudgetYearParam
    ): List<BudgetYearStatsItem> {
        return entities
            .sortedWith(compareBy<BudgetEntity> { it.month }.thenByDescending { it.amount })
            .drop(param.offset)
            .take(param.limit)
            .map { it.toStatsItem() }
    }
}