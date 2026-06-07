package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogConf {

    private String rootLevel = "WARN";
    private String dns = "INFO";
    private String cache = "WARN";
    private String infra = "WARN";
    private LogFileConf file;

    public String getRootLevel() {
        return rootLevel;
    }

    public void setRootLevel(String rootLevel) {
        this.rootLevel = rootLevel;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getInfra() {
        return infra;
    }

    public void setInfra(String infra) {
        this.infra = infra;
    }

    public LogFileConf getFile() {
        return file;
    }

    public void setFile(LogFileConf file) {
        this.file = file;
    }

}
