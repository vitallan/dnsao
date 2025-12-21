package com.allanvital.dnsao.dns.processor.pre.handler.opt;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OptBasicInfoProvider {

    public int getUdpPayloadSize() {
        return Constants.DEFAULT_UDP_PAYLOAD;
    }

    public int getXrCode() {
        return 0;
    }

    public int getVersion() {
        return 0;
    }

}
