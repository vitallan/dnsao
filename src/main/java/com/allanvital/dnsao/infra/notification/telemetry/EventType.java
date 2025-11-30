package com.allanvital.dnsao.infra.notification.telemetry;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public enum EventType {

    CACHE_HIT,
    CACHE_ADDED,
    CACHE_REWARM,
    CACHE_REWARM_EXPIRED,
    CACHE_REWARM_FAILED,
    REFRESHED_LISTS,
    CACHE_REWARM_NO_TTL,
    STALE_CACHE_HIT,
    REWARM_TASK_SCHEDULED,
    QUERY_RESOLVED,
    QUERY_EVENT_HTTP_NOTIFIED,
    UPSTREAM_PRIORITIZED

}
