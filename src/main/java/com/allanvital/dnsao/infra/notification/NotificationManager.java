package com.allanvital.dnsao.infra.notification;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.utils.ExceptionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class NotificationManager {


    private final AtomicReference<List<QueryEventListener>> queryListeners = new AtomicReference<>(new LinkedList<>());

    private boolean queryLogEnabled = true;

    public NotificationManager(boolean queryLogEnabled) {
        this.queryLogEnabled = queryLogEnabled;
    }

    public void notifyQuery(QueryEvent queryEvent) {
        if (!queryLogEnabled) {
            queryEvent.anonymize();
        }
        for (QueryEventListener listener : queryListeners.get()) {
            try {
                listener.receiveNewQuery(queryEvent);
            } catch (Throwable t) {
                if (t.getCause() instanceof InterruptedException) {
                    throw t;
                }
                Throwable rootCause = ExceptionUtils.findRootCause(t);
                Log.INFRA.error("failed to notify listener {} : {}", listener, rootCause.getMessage());
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