package com.allanvital.dnsao.cache.keep;

import com.allanvital.dnsao.cache.pojo.KeepEntry;
import com.allanvital.dnsao.conf.inner.CacheConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.allanvital.dnsao.infra.AppLoggers.CACHE;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class KeepProvider {

    private static final Logger log = LoggerFactory.getLogger(CACHE);

    private final List<KeepEntry> keep = new LinkedList<>();

    private static final Set<Integer> KEEP_TYPES = Set.of(
            Type.A,
            Type.AAAA,
            Type.CNAME,
            Type.MX,
            Type.NS,
            Type.HTTPS
    );

    public KeepProvider(CacheConf cacheConf) {
        List<String> urls = cacheConf.getKeep();
        for (String url : urls) {
            for (Integer type : KEEP_TYPES) {
                try {
                    keep.add(new KeepEntry(Record.newRecord(getName(url), type, DClass.IN)));
                } catch (TextParseException e) {
                    log.warn("it was not possible to parse {} as a uri to be kickstarted, ignoring", url);
                }
            }
        }
    }

    public Name getName(String url) throws TextParseException {
        if (!url.endsWith(".")) {
            url = url + ".";
        }
        return Name.fromString(url);
    }

    public List<KeepEntry> getUrlsToKeep() {
        return keep;
    }

    public boolean contain(Record record) {
        KeepEntry toCheck = new KeepEntry(record);
        return keep.contains(toCheck);
    }

}
