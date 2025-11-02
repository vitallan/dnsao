package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface PreHandler {

    //root name (1) + TYPE (2) + CLASS/udpPayload (2) + TTL/flags+ver (4) + RDLEN (2)
    int OPT_FIXED_OVERHEAD = 11;
    //OPTION-CODE (2) + OPTION-LENGTH (2)
    int EDNS_OPTION_OVERHEAD = 4;
    int DEFAULT_UDP_PAYLOAD = 1232;
    int DEFAULT_BLOCK_SIZE = 128;

    Message prepare(Message message) throws PreHandlerException;

}
