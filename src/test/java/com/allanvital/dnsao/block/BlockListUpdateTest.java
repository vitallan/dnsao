package com.allanvital.dnsao.block;

import com.allanvital.dnsao.exc.ConfException;
import com.allanvital.dnsao.graph.bean.FakeDownloadDomainListFileReader;
import com.allanvital.dnsao.graph.bean.TestRefresher;
import com.allanvital.dnsao.holder.TestHolder;
import com.allanvital.dnsao.infra.notification.telemetry.EventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class BlockListUpdateTest extends TestHolder {

    FakeDownloadDomainListFileReader fileReader;
    TestRefresher refresher;

    @BeforeEach
    public void setup() throws ConfException {
        fileReader = new FakeDownloadDomainListFileReader();
        refresher = new TestRefresher();
        registerOverride(fileReader, refresher);
        safeStart("1udp-refresh-lists.yml");
        assertEquals(1, fileReader.getReadEntriesCallCount());
    }

    @Test
    public void shouldUpdateListsFromTimeToTimeWhenEnabled() throws Exception {
        refresher.manualRefresh();
        eventListener.assertCount(EventType.REFRESHED_LISTS, 2, false);
        assertEquals(2, fileReader.getReadEntriesCallCount());
        refresher.manualRefresh();
        eventListener.assertCount(EventType.REFRESHED_LISTS, 3, false);
        assertEquals(3, fileReader.getReadEntriesCallCount());
    }

    @AfterEach
    public void afterEach() throws InterruptedException {
        safeStop();
    }

}
