package com.allanvital.dnsao.web.stats.db;

import com.allanvital.dnsao.infra.notification.QueryResolvedBy;
import com.allanvital.dnsao.web.stats.Bucket;

/**
 * Immutable bucket snapshot backed by the DB aggregates.
 */
public class DbBucket implements Bucket {

    private final long total;
    private final long cache;
    private final long blocked;
    private final long local;
    private final long upstream;
    private final long refused;
    private final long servfail;
    private final long recursion;

    public DbBucket(long total,
                    long cache,
                    long blocked,
                    long local,
                    long upstream,
                    long refused,
                    long servfail,
                    long recursion) {
        this.total = total;
        this.cache = cache;
        this.blocked = blocked;
        this.local = local;
        this.upstream = upstream;
        this.refused = refused;
        this.servfail = servfail;
        this.recursion = recursion;
    }

    public static DbBucket zero() {
        return new DbBucket(0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public long getCounter(QueryResolvedBy queryResolvedBy) {
        if (queryResolvedBy == null) {
            return total;
        }
        return switch (queryResolvedBy) {
            case CACHE -> cache;
            case BLOCKED -> blocked;
            case LOCAL -> local;
            case UPSTREAM -> upstream;
            case REFUSED -> refused;
            case SERVFAIL -> servfail;
            case RECURSION -> recursion;
        };
    }

    @Override
    public long getTotalCounter() {
        return total;
    }
}
