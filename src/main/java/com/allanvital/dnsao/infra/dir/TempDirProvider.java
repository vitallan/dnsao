package com.allanvital.dnsao.infra.dir;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface TempDirProvider {

    default String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }

}
