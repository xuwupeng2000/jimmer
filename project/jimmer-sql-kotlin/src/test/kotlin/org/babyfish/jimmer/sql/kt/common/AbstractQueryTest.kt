package org.babyfish.jimmer.sql.kt.common

import com.fasterxml.jackson.core.JsonProcessingException
import org.babyfish.jimmer.sql.kt.ast.query.KTypedRootQuery
import java.sql.Connection
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import kotlin.test.assertEquals
import kotlin.test.assertFalse

abstract class AbstractQueryTest : AbstractTest() {

    private var maxStatementIndex:Int = -1

    private var rows: List<Any?> = emptyList()

    protected open fun <R> executeAndExpect(
        query: KTypedRootQuery<R>,
        block: QueryTestContext<R>.() -> Unit
    ) {
        clearExecutions()
        rows = emptyList()
        maxStatementIndex = -1
        jdbc { con ->
            rows = query.execute(con)
        }
        block(QueryTestContext<R>(0))
        assertEquals(
            maxStatementIndex + 1,
            executions.size,
            "statement count"
        )
    }

    protected open fun <T> connectAndExpect(
        func: (Connection) -> T,
        block: QueryTestContext<T>.() -> Unit
    ) {
        clearExecutions()
        rows = emptyList()
        maxStatementIndex = -1
        jdbc { con: Connection ->
            if (rows.isEmpty()) {
                val result = func(con)
                rows = if (result is List<*>) {
                    result
                } else {
                    listOf(result)
                }
            }
        }
        block(QueryTestContext<T>(0))
        assertEquals(
            maxStatementIndex + 1,
            executions.size,
            "statement count"
        )
    }

    protected inner class QueryTestContext<R> internal constructor(private val index: Int) {

        fun statement(index: Int): QueryTestContext<R> {
            return QueryTestContext(index)
        }

        fun sql(sql: String) {
            maxStatementIndex = Math.max(maxStatementIndex, index)
            assertFalse(
                executions.isEmpty(),
                "Not sql history"
            )
            contentEquals(
                sql,
                executions[index].sql,
                "statements[$index].sql"
            )
        }

        fun variables(vararg variables: Any?) {
            variables(variables.toList())
        }

        fun variables(variables: List<Any?>) {
            assertFalse(
                executions.isEmpty(),
                "Not sql history"
            )
            assertEquals(
                variables,
                executions[index].variables,
                "statements[$index].variables"
            )
        }

        fun variables(variables: Set<Any?>) {
            assertFalse(
                executions.isEmpty(),
                "Not sql history"
            )
            assertEquals(
                variables,
                executions[index].variables.toSet(),
                "statements[$index].variables"
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun rows(block: (List<R>) -> Unit) {
            block(rows as List<R>)
        }

        fun rows(rows: List<R>) {
            assertEquals(
                rows,
                this@AbstractQueryTest.rows
            )
        }

        fun row(index: Int, consumer: Consumer<R>): QueryTestContext<R> {
            consumer.accept(rows.get(index) as R)
            return this
        }

        fun rows(json: String): QueryTestContext<R> {
            try {
                contentEquals(json, MAPPER.writeValueAsString(rows))
            } catch (ex: JsonProcessingException) {
                throw RuntimeException(ex)
            }
            return this
        }

        fun rows(count: Int): QueryTestContext<R> {
            assertEquals(count, rows.size)
            return this
        }
    }
}