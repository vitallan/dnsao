package com.allanvital.dnsao;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface Constants {

    long STATS_WINDOW_MS = 24 * 60 * 60_000L;
    long STATS_BUCKET_INTERVAL_MS = 10 * 60_000L;
    String DB_DEFAULT_NAME = "dnsao.db";
    int DEFAULT_DNS_PORT = 53;

}
