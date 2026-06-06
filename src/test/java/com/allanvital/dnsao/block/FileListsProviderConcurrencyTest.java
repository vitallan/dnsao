package com.allanvital.dnsao.block;

import com.allanvital.dnsao.conf.inner.ListsConf;
import com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf;
import com.allanvital.dnsao.dns.block.BlockDecider;
import com.allanvital.dnsao.dns.block.FileListsProvider;
import com.allanvital.dnsao.graph.bean.CyclingReader;
import com.allanvital.dnsao.graph.bean.TestRefresher;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.Name;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.conf.inner.pojo.GroupInnerConf.MAIN;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class FileListsProviderConcurrencyTest {

    @Test
    void shouldHandleConcurrentReadsDuringRefresh() throws Exception {
        CyclingReader cyclingReader = new CyclingReader();
        ListsConf listsConf = new ListsConf();
        listsConf.setAllowLists(Map.of("allow1", "urlAllow1"));
        listsConf.setBlockLists(Map.of("block1", "urlBlock1"));
        TestRefresher refresher = new TestRefresher();

        FileListsProvider provider = new FileListsProvider(refresher, listsConf, cyclingReader, true);

        GroupInnerConf mainGroup = new GroupInnerConf();
        mainGroup.setAllows(Set.of("allow1"));
        mainGroup.setBlocks(Set.of("block1"));
        Map<String, GroupInnerConf> groups = new HashMap<>();
        groups.put(MAIN, mainGroup);

        BlockDecider decider = new BlockDecider(provider, listsConf, groups);

        int readerThreads = 4;
        ExecutorService readerPool = Executors.newFixedThreadPool(readerThreads);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable lookupTask = () -> {
            try {
                InetAddress client = InetAddress.getByName("127.0.0.1");
                for (int i = 0; i < 5000; i++) {
                    Name name = Name.fromString("domain-" + (i % 100) + ".com");
                    decider.isBlocked(client, name);
                }
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        };

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < readerThreads; i++) {
            futures.add(readerPool.submit(lookupTask));
        }

        for (int i = 0; i < 100; i++) {
            refresher.manualRefresh();
            Thread.yield();
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        readerPool.shutdown();

        assertNull(error.get(), "Concurrent access failed: " + error.get());
    }

}
