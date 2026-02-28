package com.allanvital.dnsao.web.stats;

import com.allanvital.dnsao.infra.notification.QueryResolvedBy;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface Bucket {

    long getCounter(QueryResolvedBy queryResolvedBy);
    long getTotalCounter();

}
