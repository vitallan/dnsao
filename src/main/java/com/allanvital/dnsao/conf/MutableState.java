package com.allanvital.dnsao.conf;

import java.util.concurrent.atomic.AtomicBoolean;

public class MutableState {

    private final AtomicBoolean blockingEnabled;

    public MutableState(boolean blockingEnabled) {
        this.blockingEnabled = new AtomicBoolean(blockingEnabled);
    }

    public boolean isBlockingEnabled() {
        return blockingEnabled.get();
    }

    public void setBlockingEnabled(boolean enabled) {
        blockingEnabled.set(enabled);
    }

}
