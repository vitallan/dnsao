package com.allanvital.dnsao.infra.dir;

import com.allanvital.dnsao.infra.dir.TempDirProvider;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class TestTempDirProvider implements TempDirProvider {

    @Override
    public String getTempDir() {
        return "data";
    }

}
