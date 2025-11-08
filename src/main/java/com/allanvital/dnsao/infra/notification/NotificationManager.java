package com.allanvital.dnsao.infra.notification;

import com.allanvital.dnsao.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.allanvital.dnsao.infra.AppLoggers.INFRA;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NotificationManager {

    private static final Logger log = LoggerFactory.getLogger(INFRA);

    private final AtomicReference<List<QueryEventListener>> queryListeners = new AtomicReference<>(new LinkedList<>());

    private boolean queryLogEnabled = true;

    public NotificationManager(boolean queryLogEnabled) {
        this.queryLogEnabled = queryLogEnabled;
    }

    public void notifyQuery(QueryEvent queryEvent) {
        if (!queryLogEnabled) {
            return;
        }
        for (QueryEventListener listener : queryListeners.get()) {
            try {
                listener.receiveNewQuery(queryEvent);
            } catch (Throwable t) {
                if (t.getCause() instanceof InterruptedException) {
                    throw t;
                }
                Throwable rootCause = ExceptionUtils.findRootCause(t);
                log.error("failed to notify listener " + listener + " : " + rootCause.getMessage());
            }
        }
    }

    public void querySubscribe(QueryEventListener listener) {
        List<QueryEventListener> queryEventListeners = queryListeners.get();
        queryEventListeners.add(listener);
        this.queryListeners.set(queryEventListeners);
    }

    public void queryUnsubscribe(QueryEventListener listener) {
        List<QueryEventListener> queryEventListeners = queryListeners.get();
        queryEventListeners.remove(listener);
        this.queryListeners.set(queryEventListeners);
    }

}