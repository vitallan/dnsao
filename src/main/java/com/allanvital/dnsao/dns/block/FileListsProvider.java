package com.allanvital.dnsao.dns.block;

import com.allanvital.dnsao.conf.inner.ListsConf;
import com.allanvital.dnsao.infra.clock.Clock;
import org.xbill.DNS.Name;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.infra.notification.telemetry.EventType.REFRESHED_LISTS;
import static com.allanvital.dnsao.infra.notification.telemetry.TelemetryEventManager.telemetryNotify;
import static com.allanvital.dnsao.utils.HashUtils.fnv1a64;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class FileListsProvider {

    private static final int REFRESH_INTERVAL_IN_HOURS = 12;
    private long lastBeat = Clock.currentTimeInMillis();

    private final AtomicReference<Map<String, Set<Long>>> domainsUnderListName = new AtomicReference<>(new HashMap<>());
    private final ListsConf listsConf;
    private final DomainListFileReader domainListFileReader;

    public FileListsProvider(Refresher refresher, ListsConf listsConf, DomainListFileReader domainListFileReader, boolean refreshLists) {
        this.listsConf = listsConf;
        this.domainListFileReader = domainListFileReader;
        this.populate();
        if (refreshLists) {
            Runnable task = this::populate;
            refresher.scheduleRefresh(task);
        }
    }

    private void populate() {
        populateMap(listsConf.getAllowLists());
        populateMap(listsConf.getBlockLists());
    }

    private void populateMap(Map<String, String> toUse) {
        Map<String, Set<Long>> domainsMap = domainsUnderListName.get();
        Set<Map.Entry<String, String>> entries = toUse.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String url = entry.getValue();
            Set<Long> parsedUrls = domainListFileReader.readEntries(url);
            telemetryNotify(REFRESHED_LISTS);
            domainsMap.put(name, parsedUrls);
        }
        domainsUnderListName.set(domainsMap);
    }

    public boolean isInBlocked(Name name, Set<String> blockNames) {
        Map<String, Set<Long>> map = domainsUnderListName.get();
        for (String blockName : blockNames) {
            if (isBlocked(name, map.get(blockName))) {
                return true;
            }
        }
        return false;
    }

    public boolean isInAllowed(Name name, Set<String> allowedNames) {
        Map<String, Set<Long>> map = domainsUnderListName.get();
        for (String allowedName : allowedNames) {
            if (isAllowed(name, map.get(allowedName))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(Name qname) {
        String fqdn = qname.toString(true).toLowerCase(Locale.ROOT);
        if (fqdn.endsWith(".")) {
            fqdn = fqdn.substring(0, fqdn.length() - 1);
        }
        return fqdn;
    }

    private boolean isAllowed(Name name, Set<Long> allowedSet) {
        return allowedSet.contains(fnv1a64(normalize(name)));
    }

    private boolean isBlocked(Name qname, Set<Long> blockedSet) {
        String fqdn = normalize(qname);
        Long hash = fnv1a64(fqdn);
        if (blockedSet.contains(hash)) return true;
        int dot = fqdn.indexOf('.');
        while (dot != -1 && dot + 1 < fqdn.length()) {
            String suffix = fqdn.substring(dot + 1);
            Long suffixHash = fnv1a64(suffix);
            if (blockedSet.contains(suffixHash)) return true;
            dot = fqdn.indexOf('.', dot + 1);
        }
        return false;
    }

}
