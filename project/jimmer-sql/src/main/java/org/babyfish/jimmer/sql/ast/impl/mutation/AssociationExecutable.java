package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.runtime.Selectors;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.sql.Connection;
import java.util.*;

class AssociationExecutable implements Executable<Integer> {

    final JSqlClient sqlClient;

    final Connection con;

    private final AssociationType associationType;

    private final boolean reversed;

    private final Mode mode;

    private final Set<Tuple2<Object, Object>> idTuples;

    public AssociationExecutable(
            JSqlClient sqlClient,
            Connection con,
            AssociationType associationType,
            boolean reversed,
            Mode mode,
            Collection<Tuple2<Object, Object>> idTuples
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.associationType = associationType;
        this.reversed = reversed;
        this.mode = mode;
        this.idTuples = idTuples instanceof Set<?> ?
                (Set<Tuple2<Object, Object>>)idTuples :
                new LinkedHashSet<>(idTuples);
    }

    @NewChain
    public AssociationExecutable setMode(Mode mode) {
        if (this.mode == mode) {
            return this;
        }
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                mode,
                idTuples
        );
    }

    @Override
    public Integer execute() {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public Integer execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        if (this.con != null) {
            return executeImpl(this.con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    private Integer executeImpl(Connection con) {

        if (idTuples.isEmpty()) {
            return 0;
        }

        if (mode == Mode.DELETE) {
            return getMiddleTypeOperator(con).remove(new TupleReader(idTuples));
        }

        Set<Tuple2<Object, Object>> addingPairs = idTuples;
        if (mode == Mode.CHECK_AND_INSERT) {
            addingPairs = new LinkedHashSet<>(addingPairs);
            Set<Tuple2<Object, Object>> existingPairs = new HashSet<>(find(con));
            addingPairs.removeAll(existingPairs);
            if (addingPairs.isEmpty()) {
                return 0;
            }
        }
        return getMiddleTypeOperator(con).add(new TupleReader(addingPairs));
    }

    public enum Mode {
        CHECK_AND_INSERT,
        INSERT,
        DELETE
    }

    private List<Tuple2<Object, Object>> find(Connection con) {

        MiddleTable middleTable = reversed ?
                associationType.getMiddleTable().getInverse() :
                associationType.getMiddleTable();
        Tuple2<Expression<?>, Expression<?>> expressionPair = getExpressionPair();

        SqlBuilder builder = new SqlBuilder(sqlClient);
        builder
                .sql("select ")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(" from ")
                .sql(associationType.getTableName())
                .sql(" where (")
                .sql(middleTable.getJoinColumnName())
                .sql(", ")
                .sql(middleTable.getTargetJoinColumnName())
                .sql(") in(");
        String separator = "";
        for (Tuple2<Object, Object> idTuple : idTuples) {
            builder
                    .sql(separator)
                    .sql("(")
                    .variable(idTuple.get_1())
                    .sql(", ")
                    .variable(idTuple.get_2())
                    .sql(")");
            separator = ", ";
        }
        builder.sql(")");

        Tuple2<String, List<Object>> sqlResult = builder.build();
        return Selectors.select(
                sqlClient,
                con,
                sqlResult.get_1(),
                sqlResult.get_2(),
                Arrays.asList(expressionPair.get_1(), expressionPair.get_2())
        );
    }

    private Tuple2<Expression<?>, Expression<?>> getExpressionPair() {
        Class<?> srcType = associationType.getSourceType().getIdProp().getElementClass();
        Class<?> tgtType = associationType.getTargetType().getIdProp().getElementClass();
        if (reversed) {
            return new Tuple2<>(
                    Expression.any().nullValue(tgtType),
                    Expression.any().nullValue(srcType)
            );
        }
        return new Tuple2<>(
                Expression.any().nullValue(srcType),
                Expression.any().nullValue(tgtType)
        );
    }

    private MiddleTableOperator getMiddleTypeOperator(Connection con) {
        return new MiddleTableOperator(
                sqlClient,
                con,
                reversed ?
                        associationType.getMiddleTable().getInverse() :
                        associationType.getMiddleTable(),
                (reversed ? associationType.getSourceType() : associationType.getTargetType())
                        .getIdProp()
                        .getElementClass()
        );
    }

    private static class TupleReader implements MiddleTableOperator.IdPairReader {

        private Iterator<Tuple2<Object, Object>> idTupleItr;

        private Tuple2<Object, Object> currentIdPair;

        TupleReader(Collection<Tuple2<Object, Object>> idTuples) {
            idTupleItr = idTuples.iterator();
        }

        @Override
        public boolean read() {
            if (idTupleItr.hasNext()) {
                currentIdPair = idTupleItr.next();
                return true;
            }
            return false;
        }

        @Override
        public Object sourceId() {
            return currentIdPair.get_1();
        }

        @Override
        public Object targetId() {
            return currentIdPair.get_2();
        }
    }
}
