package com.allanvital.dnsao.dns.block;

import com.allanvital.dnsao.utils.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.dns.block.ListType.ALLOW;
import static com.allanvital.dnsao.dns.block.ListType.BLOCK;
import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockListProvider {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final List<String> allowListUrls;
    private final List<String> blockListUrl;
    private final FileHandler fileHandler;
    private final AtomicReference<Set<Long>> blockListRef = new AtomicReference<>(Set.of());
    private final AtomicReference<Set<Long>> allowListRef = new AtomicReference<>(Set.of());

    public BlockListProvider(ScheduledExecutorService scheduler, List<String> allowListUrls, List<String> blockListUrl, FileHandler fileHandler, boolean refreshLists) {
        this.allowListUrls = allowListUrls;
        this.blockListUrl = blockListUrl;
        this.fileHandler = fileHandler;
        fillLists();
        if (refreshLists) {
            Runnable task = this::fillLists;
            scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.HOURS);
        }
    }

    private void fillLists() {
        log.debug("running download and blockList update");
        fileHandler.downloadFiles(allowListUrls, ALLOW);
        fileHandler.downloadFiles(blockListUrl, BLOCK);
        Set<Long> allowList = new HashSet<>(fileHandler.readAllEntriesOfType(ALLOW));
        Set<Long> blockList = new HashSet<>(fileHandler.readAllEntriesOfType(BLOCK));
        blockList.removeAll(allowList);

        blockListRef.set(Collections.unmodifiableSet(blockList));
        allowListRef.set(Collections.unmodifiableSet(allowList));
    }

    public Set<Long> getBlockList() {
        return blockListRef.get();
    }

    public boolean isBlocked(Name name) {
        if (this.allowListRef.get().contains(HashUtils.fnv1a64(normalize(name)))) {
            return false;
        }
        return isBlocked(name, this.blockListRef.get());
    }

    private String normalize(Name qname) {
        String fqdn = qname.toString(true).toLowerCase(Locale.ROOT);
        if (fqdn.endsWith(".")) {
            fqdn = fqdn.substring(0, fqdn.length() - 1);
        }
        return fqdn;
    }

    private boolean isBlocked(Name qname, Set<Long> blockedSet) {
        String fqdn = normalize(qname);
        Long hash = HashUtils.fnv1a64(fqdn);
        if (blockedSet.contains(hash)) return true;
        int dot = fqdn.indexOf('.');
        while (dot != -1 && dot + 1 < fqdn.length()) {
            String suffix = fqdn.substring(dot + 1);
            Long suffixHash = HashUtils.fnv1a64(suffix);
            if (blockedSet.contains(suffixHash)) return true;
            dot = fqdn.indexOf('.', dot + 1);
        }
        return false;
    }

}
