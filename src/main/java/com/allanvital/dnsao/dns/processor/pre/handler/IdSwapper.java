package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;

import java.security.SecureRandom;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class IdSwapper implements PreHandler {

    private static final int MAX = 1 << 16;
    private final SecureRandom rnd = new SecureRandom();

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        int id = rnd.nextInt(MAX);
        message.getHeader().setID(id);
        return message;
    }

}
