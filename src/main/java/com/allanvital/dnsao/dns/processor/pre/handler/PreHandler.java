package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public interface PreHandler {

    Message prepare(Message message) throws PreHandlerException;

}
