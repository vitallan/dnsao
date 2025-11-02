package com.allanvital.dnsao.infra.notification;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface QueryEventListener {

    void receiveNewQuery(QueryEvent queryEvent);

}