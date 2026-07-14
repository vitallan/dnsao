package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

import com.allanvital.dnsao.dns.pojo.DnsQueryRequest;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.AuthorityEndpoint;
import com.allanvital.dnsao.dns.processor.engine.unit.recursive.pojo.RecursiveResult;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Section;

import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveSession {

    protected final DnsQueryRequest dnsQueryRequest;
    protected final List<AuthorityEndpoint> rootHints;
    protected final AuthorityQueryClient authorityQueryClient;
    protected final ReferralInterpreter referralInterpreter;

    public RecursiveSession(DnsQueryRequest dnsQueryRequest,
                            List<AuthorityEndpoint> rootHints,
                            AuthorityQueryClient authorityQueryClient,
                            ReferralInterpreter referralInterpreter) {
        this.dnsQueryRequest = dnsQueryRequest;
        this.rootHints = List.copyOf(rootHints);
        this.authorityQueryClient = authorityQueryClient;
        this.referralInterpreter = referralInterpreter;
    }

    public RecursiveResult resolve() {
        Message servfail = buildServfail(dnsQueryRequest.getRequest());
        return RecursiveResult.servfail(servfail, "recursive_session_not_implemented_yet");
    }

    private Message buildServfail(Message query) {
        Message fail = new Message(query.getHeader().getID());
        fail.getHeader().setFlag(Flags.QR);
        fail.addRecord(query.getQuestion(), Section.QUESTION);
        fail.getHeader().setRcode(Rcode.SERVFAIL);
        return fail;
    }
}
