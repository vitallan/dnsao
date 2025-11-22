package com.allanvital.dnsao.conf.inner;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HttpListenerConf {

    private Set<String> http = new HashSet<>();

    public Set<String> getHttp() {
        return http;
    }

    public void setHttp(Set<String> http) {
        this.http = http;
    }

    public void addListener(String url) {
        http.add(url);
    }

    public void sanitizeHttp() {
        http.removeIf(entry -> !entry.toLowerCase().startsWith("http"));
    }

}
