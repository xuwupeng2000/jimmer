package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class InSubQueryPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private TypedSubQuery<?> subQuery;

    private boolean negative;

    public InSubQueryPredicate(
            Expression<?> expression,
            TypedSubQuery<?> subQuery,
            boolean negative
    ) {
        this.expression = expression;
        this.subQuery = subQuery;
        this.negative = negative;
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new InSubQueryPredicate(
                expression,
                subQuery,
                !negative
        );
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
        ((Ast) subQuery).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        renderChild((Ast) expression, builder);
        builder.sql(negative ? " not in " : " in ");
        renderChild((Ast) subQuery, builder);
    }
}
