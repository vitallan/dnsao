package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Section;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OptCleaner implements PreHandler {

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        OPTRecord old = message.getOPT();
        if (old != null) {
            message.removeRecord(old, Section.ADDITIONAL);
        }
        return message;
    }

}
