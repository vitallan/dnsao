package com.allanvital.dnsao.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlwaysRewarmTopEntriesTrackerTest {

    @Test
    void shouldKeepTopEntriesByLatestAccess() {
        AlwaysRewarmTopEntriesTracker tracker = new AlwaysRewarmTopEntriesTracker(2);

        tracker.recordAccess("a");
        tracker.recordAccess("b");
        tracker.recordAccess("c");
        tracker.recordAccess("a");

        assertTrue(tracker.isProtected("a"));
        assertTrue(tracker.isProtected("c"));
        assertFalse(tracker.isProtected("b"));
        assertEquals(List.of("a", "c"), tracker.snapshotProtectedKeys());
    }

    @Test
    void shouldReturnAllTrackedEntriesWhenThresholdIsLargerThanPopulation() {
        AlwaysRewarmTopEntriesTracker tracker = new AlwaysRewarmTopEntriesTracker(10);

        tracker.recordAccess("a");
        tracker.recordAccess("b");

        assertEquals(List.of("b", "a"), tracker.snapshotProtectedKeys());
    }

    @Test
    void shouldRemoveEntriesFromProtectedSet() {
        AlwaysRewarmTopEntriesTracker tracker = new AlwaysRewarmTopEntriesTracker(2);

        tracker.recordAccess("a");
        tracker.recordAccess("b");
        tracker.remove("b");

        assertFalse(tracker.isProtected("b"));
        assertEquals(List.of("a"), tracker.snapshotProtectedKeys());
    }
}
