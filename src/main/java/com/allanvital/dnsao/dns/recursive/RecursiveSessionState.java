package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.Name;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Allan Vital (https://allanvital.com)
 */
class RecursiveSessionState {

    private final int maxNsNameResolutions;
    private final long maxSessionElapsedMillis;
    private int stepCount;
    private int helperResolutionCount;
    private long sessionDeadlineNs;
    private boolean helperBudgetLogged;
    private boolean sessionBudgetLogged;
    private long sessionId;
    private Name sessionQname;
    private final Set<String> seenHelperKeys = new HashSet<>();

    RecursiveSessionState(int maxNsNameResolutions, long maxSessionElapsedMillis) {
        this.maxNsNameResolutions = maxNsNameResolutions;
        this.maxSessionElapsedMillis = maxSessionElapsedMillis;
    }

    void start(long sessionId, Name sessionQname) {
        this.sessionId = sessionId;
        this.sessionQname = sessionQname;
        this.stepCount = 0;
        this.helperResolutionCount = 0;
        this.helperBudgetLogged = false;
        this.sessionBudgetLogged = false;
        this.seenHelperKeys.clear();
        this.sessionDeadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxSessionElapsedMillis);
    }

    void recordStep() {
        stepCount++;
    }

    int stepCount() {
        return stepCount;
    }

    boolean isHelperBudgetExhausted() {
        return helperResolutionCount >= maxNsNameResolutions;
    }

    boolean tryAcquireHelperResolutionBudget(Name target) {
        if (helperResolutionCount < maxNsNameResolutions) {
            helperResolutionCount++;
            return true;
        }
        if (!helperBudgetLogged) {
            helperBudgetLogged = true;
            Log.DNS.trace("recursive helper budget exhausted session={} qname={} maxHelperResolutions={} target={}", sessionId, sessionQname, maxNsNameResolutions, target);
        }
        return false;
    }

    boolean tryHelperAttempt(Name target, int qtype) {
        return seenHelperKeys.add(target + ":" + qtype);
    }

    boolean hasRemainingSessionBudget() {
        if (System.nanoTime() <= sessionDeadlineNs) {
            return true;
        }
        if (!sessionBudgetLogged) {
            sessionBudgetLogged = true;
            Log.DNS.trace("recursive wall clock budget exhausted session={} qname={} maxElapsedMs={}", sessionId, sessionQname, maxSessionElapsedMillis);
        }
        return false;
    }
}
