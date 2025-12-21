package com.allanvital.dnsao.dns.processor.pre.handler.opt;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.xbill.DNS.ExtendedFlags;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.OFF;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OptFlagProvider {

    private final DNSSecMode dnsSecMode;

    public OptFlagProvider(DNSSecMode dnsSecMode) {
        this.dnsSecMode = dnsSecMode;
    }

    public int getFlags() {
        if (OFF.equals(dnsSecMode)) {
            return 0;
        }
        return ExtendedFlags.DO;
    }

}
