package com.allanvital.dnsao.dns.processor.pre.handler.opt;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface Constants {

    //root name (1) + TYPE (2) + CLASS/udpPayload (2) + TTL/flags+ver (4) + RDLEN (2)
    int OPT_FIXED_OVERHEAD = 11;
    //OPTION-CODE (2) + OPTION-LENGTH (2)
    int EDNS_OPTION_OVERHEAD = 4;
    int DEFAULT_UDP_PAYLOAD = 1232;
    int DEFAULT_BLOCK_SIZE = 128;
    int SIMPLE_CAP_BYTES = 96;

}
