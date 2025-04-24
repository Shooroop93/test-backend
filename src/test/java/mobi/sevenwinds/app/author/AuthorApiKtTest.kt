package mobi.sevenwinds.app.author

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction {
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testCreateAuthorSuccessfully() {
        val fio = "Тестов Тест Тестович"
        val record = AuthorRecord(fio)

        RestAssured.given()
            .jsonBody(record)
            .post("/author/add")
            .toResponse<AuthorRecord>().let { response ->
                assertEquals(record.fio, response.fio)
            }
    }

    @Test
    fun testFailOnTooShortFio() {
    val record = AuthorRecord("т!") //

        RestAssured.given()
            .jsonBody(record)
            .post("/author/add")
            .then()
            .statusCode(400)
    }
}