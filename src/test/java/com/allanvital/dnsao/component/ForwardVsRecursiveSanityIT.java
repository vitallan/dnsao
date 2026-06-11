package com.allanvital.dnsao.component;

import com.allanvital.dnsao.dns.DnsServer;
import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.holder.TestHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ForwardVsRecursiveSanityIT extends TestHolder {

    private static final List<String> DOMAINS = List.of(
            "example.com",
            "example.net",
            "example.org",
            "iana.org",
            "www.iana.org",
            "iana-servers.net",
            "internic.net",
            "www.internic.net",
            "allanvital.com",
            "devzao.com"
    );

    private static final List<Integer> QTYPES = List.of(Type.A, Type.AAAA);
    private static final String FORWARD_CONFIG = "forward-mode-quad9.yml";
    private static final String RECURSIVE_CONFIG = "recursive-mode-live.yml";

    @Override
    protected void setupSslStore() {
    }

    @Override
    protected void setRootHints() throws ConfException {
    }

    @Test
    public void forwardAndRecursiveReturnSameNormalizedAnswers() throws Exception {
        Map<QueryKey, NormalizedResult> forwardResults = runScenario(FORWARD_CONFIG);
        Map<QueryKey, NormalizedResult> recursiveResults = runScenario(RECURSIVE_CONFIG);

        List<String> mismatches = collectMismatches(forwardResults, recursiveResults);
        if (!mismatches.isEmpty()) {
            fail("Forward vs recursive sanity mismatches:\n\n" + String.join("\n\n", mismatches));
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        safeStop();
    }

    private Map<QueryKey, NormalizedResult> runScenario(String configResource) throws Exception {
        loadConf(configResource);
        conf.getMisc().setQueryLog(false);
        safeStartWithPresetConf(true);

        try {
            Map<QueryKey, NormalizedResult> results = new LinkedHashMap<>();
            for (String domain : DOMAINS) {
                for (Integer qtype : QTYPES) {
                    QueryKey queryKey = new QueryKey(domain, qtype);
                    results.put(queryKey, queryAndNormalize(domain, qtype));
                }
            }
            return results;
        } finally {
            safeStop();
        }
    }

    private NormalizedResult queryAndNormalize(String domain, int qtype) {
        try {
            Message request = buildQuery(domain, qtype);
            Message response = executeRequestOnOwnServer(request);
            if (response == null) {
                return new NormalizedResult(qtype, Rcode.string(Rcode.SERVFAIL), List.of(), "null response");
            }
            return new NormalizedResult(qtype, Rcode.string(response.getRcode()), extractAnswerData(response, qtype), null);
        } catch (Exception e) {
            return new NormalizedResult(qtype, null, List.of(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Message buildQuery(String domain, int qtype) throws TextParseException {
        String fqdn = domain.endsWith(".") ? domain : domain + ".";
        return Message.newQuery(Record.newRecord(Name.fromString(fqdn), qtype, DClass.IN));
    }

    private List<String> extractAnswerData(Message response, int qtype) throws IOException {
        Set<String> values = new LinkedHashSet<>();
        for (Record record : response.getSection(Section.ANSWER)) {
            if (record.getType() != qtype) {
                continue;
            }
            if (record instanceof ARecord aRecord) {
                values.add(aRecord.getAddress().getHostAddress());
            } else if (record instanceof AAAARecord aaaaRecord) {
                values.add(aaaaRecord.getAddress().getHostAddress());
            }
        }
        return values.stream().sorted().toList();
    }

    private List<String> collectMismatches(Map<QueryKey, NormalizedResult> forwardResults,
                                           Map<QueryKey, NormalizedResult> recursiveResults) {
        List<String> mismatches = new ArrayList<>();
        for (String domain : DOMAINS) {
            for (Integer qtype : QTYPES) {
                QueryKey queryKey = new QueryKey(domain, qtype);
                NormalizedResult forwardResult = forwardResults.get(queryKey);
                NormalizedResult recursiveResult = recursiveResults.get(queryKey);
                if (!forwardResult.equals(recursiveResult)) {
                    mismatches.add(domain + " " + Type.string(qtype)
                            + "\nFORWARD: " + forwardResult
                            + "\nRECURSIVE: " + recursiveResult);
                }
            }
        }
        return mismatches;
    }

    private record QueryKey(String domain, int qtype) {
    }

    private record NormalizedResult(int qtype, String rcode, List<String> answers, String error) {
    }
}
