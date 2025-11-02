package com.allanvital.dnsao.infra.notification;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public enum EventType {

    CACHE_HIT,
    CACHE_REWARM,
    CACHE_REWARM_EXPIRED,
    STALE_CACHE_HIT,
    REWARM_TASK_SCHEDULED,
    QUERY_RESOLVED,

}
