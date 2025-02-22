package org.babyfish.jimmer.sql.kt.ast.expression.impl

import org.babyfish.jimmer.sql.ast.impl.Ast
import org.babyfish.jimmer.sql.ast.impl.AstVisitor
import org.babyfish.jimmer.sql.ast.impl.ExpressionPrecedences
import org.babyfish.jimmer.sql.runtime.SqlBuilder

internal abstract class CompositePredicate(
    protected val predicates: List<AbstractKPredicate>
): AbstractKPredicate() {

    override fun accept(visitor: AstVisitor) {
        for (predicate in predicates) {
            predicate.accept(visitor)
        }
    }

    override fun renderTo(builder: SqlBuilder) {
        if (predicates.isEmpty()) {
            builder.sql(emptyContent())
        } else {
            var sp: String? = null
            for (predicate in predicates) {
                if (sp !== null) {
                    builder.sql(sp)
                } else {
                    sp = operator()
                }
                renderChild(predicate as Ast, builder)
            }
        }
    }

    protected abstract fun operator(): String

    protected abstract fun emptyContent(): String
}

internal class AndPredicate(
    predicates: List<AbstractKPredicate>
): CompositePredicate(predicates) {

    override fun operator(): String = " and "

    override fun not(): AbstractKPredicate =
        OrPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.AND

    override fun emptyContent(): String = "1 = 1"
}

internal class OrPredicate(
    predicates: List<AbstractKPredicate>
): CompositePredicate(predicates) {

    override fun operator(): String = " or "

    override fun not(): AbstractKPredicate =
        AndPredicate(predicates.map { it.not() })

    override fun precedence(): Int = ExpressionPrecedences.OR

    override fun emptyContent(): String = "1 = 0"
}

internal class NotPredicate(
    private val predicate: AbstractKPredicate
) : AbstractKPredicate() {

    override fun not(): AbstractKPredicate = predicate

    override fun precedence(): Int = ExpressionPrecedences.NOT

    override fun accept(visitor: AstVisitor) {
        predicate.accept(visitor)
    }

    override fun renderTo(builder: SqlBuilder) {
        builder.sql("not ")
        usingLowestPrecedence {
            renderChild(predicate, builder)
        }
    }
}