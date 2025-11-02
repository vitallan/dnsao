package com.allanvital.dnsao.conf;

import com.allanvital.dnsao.conf.inner.ResolverConf;

import java.io.InputStream;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public abstract class ConfValidation {

    protected abstract String getFolder();

    protected Conf getConf(String file) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(getFolder() + "/" + file);
        Conf conf = ConfLoader.load(input);
        return conf;
    }

}
