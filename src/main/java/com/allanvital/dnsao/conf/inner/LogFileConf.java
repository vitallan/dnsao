package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class LogFileConf {

    private String path;
    private long maxSize = 10_485_760L;
    private int maxFiles = 5;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

}
