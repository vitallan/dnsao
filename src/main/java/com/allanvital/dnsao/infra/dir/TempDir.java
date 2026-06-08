package com.allanvital.dnsao.infra.dir;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TempDir {

    private static TempDirProvider provider = new RealTempDirProvider();

    public static String getTempDir() {
        return provider.getTempDir();
    }

    public static void setProvider(TempDirProvider newProvider) {
        provider = newProvider;
    }

}
