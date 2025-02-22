package org.babyfish.jimmer.sql.loader.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.loader.ReferenceLoader;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

class ReferenceLoaderImpl<SE, TE, TT extends Table<TE>> implements ReferenceLoader<SE, TE, TT> {

    private final JSqlClient sqlClient;

    private final Connection con;

    private final ImmutableProp prop;

    private final Filter<?> filter;

    public ReferenceLoaderImpl(
            JSqlClient sqlClient,
            ImmutableProp prop
    ) {
        this(sqlClient, null, prop, null);
    }

    public ReferenceLoaderImpl(
            JSqlClient sqlClient,
            Connection con,
            ImmutableProp prop,
            Filter<?> filter
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.prop = prop;
        this.filter = filter;
    }

    @Override
    public ReferenceLoader<SE, TE, TT> forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new ReferenceLoaderImpl<>(sqlClient, con, prop, filter);
    }

    @Override
    public ReferenceLoader<SE, TE, TT> forFilter(Filter<TT> filter) {
        if (this.filter == filter) {
            return this;
        }
        if (!prop.isNullable() && filter != null) {
            throw new IllegalArgumentException(
                    "Cannot create filterable loader for \"" + prop + "\", " +
                            "non-null association does not accept filter"
            );
        }
        return new ReferenceLoaderImpl<>(sqlClient, con, prop, filter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Executable<TE> loadCommand(SE source) {
        if (source instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "source cannot be collection, do you want to call 'batchLoadCommand'?"
            );
        }
        return new SingleCommand<>(
                sqlClient,
                con,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                Integer.MAX_VALUE,
                0,
                (ImmutableSpi) source
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Executable<Map<SE, TE>> batchLoadCommand(Collection<SE> sources) {
        return new BatchCommand<>(
                sqlClient,
                con,
                prop,
                (Filter<Table<ImmutableSpi>>) filter,
                (Collection<ImmutableSpi>) sources
        );
    }
}
