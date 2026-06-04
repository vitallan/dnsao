package com.allanvital.dnsao.infra.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationManagerTest {

    @Test
    void shouldNotifyListenersWhenQueryLogIsEnabled() {
        NotificationManager manager = new NotificationManager(true);
        List<QueryEvent> received = new ArrayList<>();
        manager.querySubscribe(received::add);

        QueryEvent event = new QueryEvent(1000L);
        manager.notifyQuery(event);

        assertEquals(1, received.size());
        assertSame(event, received.get(0));
    }

    @Test
    void shouldDeliverFullEventWhenQueryLogIsEnabled() {
        NotificationManager manager = new NotificationManager(true);
        List<QueryEvent> received = new ArrayList<>();
        manager.querySubscribe(received::add);

        QueryEvent event = new QueryEvent(QueryResolvedBy.UPSTREAM, "1.1.1.1", 5000L);
        event.setDomain("example.com.");
        event.setClient("192.168.1.1");
        event.setType("A");
        event.setAnswer("10.0.0.1");
        event.setElapsedTime(42L);

        manager.notifyQuery(event);

        assertEquals(1, received.size());
        QueryEvent receivedEvent = received.get(0);
        assertSame(event, receivedEvent);
        assertEquals("example.com.", receivedEvent.getDomain());
        assertEquals("192.168.1.1", receivedEvent.getClient());
        assertEquals("A", receivedEvent.getType());
        assertEquals("10.0.0.1", receivedEvent.getAnswer());
        assertEquals(QueryResolvedBy.UPSTREAM, receivedEvent.getQueryResolvedBy());
        assertEquals("1.1.1.1", receivedEvent.getSource());
        assertEquals(42L, receivedEvent.getElapsedTime());
    }

    @Test
    void shouldNotifyListenersWhenQueryLogIsDisabled() {
        NotificationManager manager = new NotificationManager(false);
        List<QueryEvent> received = new ArrayList<>();
        manager.querySubscribe(received::add);

        QueryEvent event = new QueryEvent(1000L);
        manager.notifyQuery(event);

        assertEquals(1, received.size(),
                "all subscribers should be notified even when queryLog is disabled");
    }

    @Test
    void shouldDeliverAnonymizedEventWhenQueryLogIsDisabled() {
        NotificationManager manager = new NotificationManager(false);
        List<QueryEvent> received = new ArrayList<>();
        manager.querySubscribe(received::add);

        QueryEvent event = new QueryEvent(QueryResolvedBy.UPSTREAM, "1.1.1.1", 5000L);
        event.setDomain("example.com.");
        event.setClient("192.168.1.1");
        event.setType("A");
        event.setAnswer("10.0.0.1");
        event.setElapsedTime(42L);

        manager.notifyQuery(event);

        assertEquals(1, received.size());
        QueryEvent receivedEvent = received.get(0);
        assertTrue(receivedEvent.isAnonymized(), "event should be marked as anonymized");
        assertNull(receivedEvent.getDomain(), "sensitive field domain should be cleared");
        assertNull(receivedEvent.getClient(), "sensitive field client should be cleared");
        assertNull(receivedEvent.getType(), "sensitive field type should be cleared");
        assertNull(receivedEvent.getAnswer(), "sensitive field answer should be cleared");
        assertEquals(QueryResolvedBy.UPSTREAM, receivedEvent.getQueryResolvedBy(),
                "counting field resolvedBy should be preserved");
        assertEquals("1.1.1.1", receivedEvent.getSource(),
                "counting field source should be preserved");
        assertEquals(5000L, receivedEvent.getTime(),
                "counting field time should be preserved");
        assertEquals(42L, receivedEvent.getElapsedTime(),
                "counting field elapsedTime should be preserved");
    }

}
