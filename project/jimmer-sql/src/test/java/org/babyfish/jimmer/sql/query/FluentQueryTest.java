package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fluent.Fluent;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class FluentQueryTest extends AbstractQueryTest {

    @Test
    public void testSimpleQuery() {

        Fluent fluent = getSqlClient().createFluent();
        BookTable book = new BookTable();

        executeAndExpect(
                fluent
                        .query(book)
                        .where(book.name().eq("GraphQL in Action"))
                        .orderBy(book.edition().asc())
                        .select(book),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "order by tb_1_.EDITION asc"
                    );
                    it.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":81.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"store\":{\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testSubQuery() {
        Fluent fluent = getSqlClient().createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        executeAndExpect(
                fluent
                        .query(book)
                        .where(
                                book.id().in(
                                        fluent
                                                .subQuery(author)
                                                .where(author.firstName().eq("Alex"))
                                                .select(author.books().id())
                                )
                        )
                        .orderBy(book.edition().desc())
                        .select(book),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where tb_1_.ID in (" +
                                    "--->select tb_3_.BOOK_ID " +
                                    "--->from AUTHOR as tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING as tb_3_ " +
                                    "--->--->on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_2_.FIRST_NAME = ?" +
                                    ") " +
                                    "order by tb_1_.EDITION desc"
                    );
                    it.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1,\"price\":50.00,\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testWildSubQuery() {
        Fluent fluent = getSqlClient().createFluent();
        BookTable book = new BookTable();
        AuthorTableEx author = new AuthorTableEx();

        executeAndExpect(
                fluent
                        .query(book)
                        .where(
                                fluent
                                        .subQuery(author)
                                        .where(author.books().eq(book))
                                        .where(author.firstName().eq("Alex"))
                                        .exists()
                        )
                        .orderBy(book.edition().desc())
                        .select(book),
                it -> {
                    it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID " +
                                    "from BOOK as tb_1_ " +
                                    "where exists (" +
                                    "--->select 1 " +
                                    "--->from AUTHOR as tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING as tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where " +
                                    "--->--->tb_3_.BOOK_ID = tb_1_.ID " +
                                    "--->and " +
                                    "--->--->tb_2_.FIRST_NAME = ?" +
                                    ") " +
                                    "order by tb_1_.EDITION desc"
                    );
                    it.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"store\":{\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"}" +
                                    "--->},{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1,\"price\":50.00,\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testAsTableEx() {
        Fluent fluent = getSqlClient().createFluent();
        BookTable book = new BookTable();

        executeAndExpect(
                fluent
                        .query(book)
                        .where(book.asTableEx().authors().firstName().eq("Alex"))
                        .select(book.id())
                        .distinct(),
                it -> {
                    it.sql(
                            "select distinct tb_1_.ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.BOOK_ID " +
                                    "inner join AUTHOR as tb_3_ on tb_2_.AUTHOR_ID = tb_3_.ID " +
                                    "where tb_3_.FIRST_NAME = ?"
                    );
                    it.rows(rows -> {
                        Assertions.assertEquals(3, rows.size());
                        Assertions.assertTrue(rows.contains(learningGraphQLId1));
                        Assertions.assertTrue(rows.contains(learningGraphQLId2));
                        Assertions.assertTrue(rows.contains(learningGraphQLId3));
                    });
                }
        );
    }

    @Test
    public void testIllegalArgument() {
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            getSqlClient().createFluent().query(new BookTableEx());
        });
        Assertions.assertEquals("Top-level query does not accept TableEx", ex.getMessage());
    }
}
