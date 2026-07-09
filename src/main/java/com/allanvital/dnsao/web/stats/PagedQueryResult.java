package com.allanvital.dnsao.web.stats;

import com.allanvital.dnsao.infra.notification.QueryEvent;

import java.util.List;

public record PagedQueryResult(List<QueryEvent> items, long total) {
}