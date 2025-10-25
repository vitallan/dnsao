package com.allanvital.dnsao.block;

import com.allanvital.dnsao.dns.remote.DnsUtils;
import com.allanvital.dnsao.utils.HashUtils;
import com.allanvital.dnsao.utils.ThreadShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.AppLoggers.INFRA;
import static com.allanvital.dnsao.block.ListType.ALLOW;
import static com.allanvital.dnsao.block.ListType.BLOCK;

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

    public BlockListProvider(List<String> allowListUrls, List<String> blockListUrl, FileHandler fileHandler) {
        this.allowListUrls = allowListUrls;
        this.blockListUrl = blockListUrl;
        this.fileHandler = fileHandler;
        fillLists();
        Runnable task = this::fillLists;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThreadShop.buildThreadFactory("block"));
        scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.HOURS);
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
        if (this.allowListRef.get().contains(HashUtils.fnv1a64(DnsUtils.normalize(name)))) {
            return false;
        }
        return DnsUtils.isBlocked(name, this.blockListRef.get());
    }

}
