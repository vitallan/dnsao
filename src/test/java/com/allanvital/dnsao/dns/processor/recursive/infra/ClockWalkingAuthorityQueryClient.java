package com.allanvital.dnsao.dns.processor.recursive.infra;

import com.allanvital.dnsao.dns.processor.engine.unit.recursive.AuthorityQueryClient;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityQueryResult;
import com.allanvital.dnsao.graph.TestTimeProvider;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ClockWalkingAuthorityQueryClient implements AuthorityQueryClient {

    private final AuthorityQueryClient delegate;
    private final TestTimeProvider testTimeProvider;
    private final long millisToWalkPerQuery;

    public ClockWalkingAuthorityQueryClient(AuthorityQueryClient delegate,
                                            TestTimeProvider testTimeProvider,
                                            long millisToWalkPerQuery) {
        this.delegate = delegate;
        this.testTimeProvider = testTimeProvider;
        this.millisToWalkPerQuery = millisToWalkPerQuery;
    }

    @Override
    public AuthorityQueryResult query(AuthorityEndpoint authorityEndpoint, Message query) {
        testTimeProvider.walkNow(millisToWalkPerQuery);
        return delegate.query(authorityEndpoint, query);
    }
}
