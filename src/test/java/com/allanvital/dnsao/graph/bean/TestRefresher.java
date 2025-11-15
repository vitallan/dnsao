package com.allanvital.dnsao.graph.bean;

import com.allanvital.dnsao.dns.block.Refresher;
import org.junit.jupiter.api.Assertions;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestRefresher implements Refresher {

    private Runnable runnable;

    @Override
    public void scheduleRefresh(Runnable task) {
        this.runnable = task;
    }

    public void manualRefresh() {
        if (runnable == null) {
            Assertions.fail("The runnable task should be setup on the application startup");
        }
        runnable.run();
    }

}
