package com.allanvital.dnsao.notification;

import com.allanvital.dnsao.utils.ExceptionUtils;
import com.allanvital.dnsao.utils.ThreadShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import static com.allanvital.dnsao.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NotificationManager {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private static volatile NotificationManager INSTANCE;

    private final CopyOnWriteArrayList<QueryEventListener> queryListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = ThreadShop.buildExecutor("notification", 5);
    private final List<EventListener> listeners = new LinkedList<>();

    private final AtomicLong droppedTasks = new AtomicLong();

    private NotificationManager() {

    }

    public static NotificationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NotificationManager();
        }
        return INSTANCE;
    }

    public void notify(EventType type) {
        for (EventListener listener : listeners) {
            listener.receiveNotification(type);
        }
    }

    public void notifyQuery(QueryEvent queryEvent) {
        try {
            executor.execute(() -> {
                for (QueryEventListener listener : queryListeners) {
                    try {
                        listener.receiveNewQuery(queryEvent);
                    } catch (Throwable t) {
                        Throwable rootCause = ExceptionUtils.findRootCause(t);
                        log.error("failed to notify listener " + listener + " : " + rootCause.getMessage());
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            droppedTasks.incrementAndGet();
        }
    }

    public void querySubscribe(QueryEventListener listener) {
        this.queryListeners.add(listener);
    }

    public void queryUnsubscribe(QueryEventListener listener) {
        this.queryListeners.remove(listener);
    }

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(EventListener listener) {
        listeners.remove(listener);
    }

    public long getDroppedTasks() { return droppedTasks.get(); }

}