package com.allanvital.dnsao.notification;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface QueryEventListener {

    void receiveNewQuery(QueryEvent queryEvent);

}