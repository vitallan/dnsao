package com.allanvital.dnsao.dns.processor.engine.unit.recursive;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveExecutionBudget {

    private int remainingSteps;

    public RecursiveExecutionBudget(int remainingSteps) {
        this.remainingSteps = Math.max(0, remainingSteps);
    }

    public boolean tryConsumeStep() {
        if (remainingSteps <= 0) {
            return false;
        }
        remainingSteps--;
        return true;
    }

    public int getRemainingSteps() {
        return remainingSteps;
    }
}
