package com.allanvital.dnsao.conf.inner;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class ServerConf {

    int port = 1053;
    int udpThreadPool = 20;
    int tcpThreadPool = 4;
    int httpThreadPool = 10;
    int webPort = 8044;
    String statsDbPath;
    boolean useMemoryStorage = false;
    String authPass = "";

    public int getHttpThreadPool() {
        return httpThreadPool;
    }

    public void setHttpThreadPool(int httpThreadPool) {
        this.httpThreadPool = httpThreadPool;
    }

    public int getWebPort() {
        return webPort;
    }

    public void setWebPort(int webPort) {
        this.webPort = webPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getStatsDbPath() {
        return statsDbPath;
    }

    public void setStatsDbPath(String statsDbPath) {
        this.statsDbPath = statsDbPath;
    }

    public boolean isUseMemoryStorage() {
        return useMemoryStorage;
    }

    public void setUseMemoryStorage(boolean useMemoryStorage) {
        this.useMemoryStorage = useMemoryStorage;
    }

    public int getUdpThreadPool() {
        return udpThreadPool;
    }

    public void setUdpThreadPool(int udpThreadPool) {
        this.udpThreadPool = udpThreadPool;
    }

    public int getTcpThreadPool() {
        return tcpThreadPool;
    }

    public void setTcpThreadPool(int tcpThreadPool) {
        this.tcpThreadPool = tcpThreadPool;
    }

    public String getAuthPass() {
        return authPass;
    }

    public void setAuthPass(String authPass) {
        this.authPass = authPass;
    }

}
