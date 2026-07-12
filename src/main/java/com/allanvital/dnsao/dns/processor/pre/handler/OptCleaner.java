package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OptCleaner implements PreHandler {

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        List<Record> additional = new ArrayList<>(message.getSection(Section.ADDITIONAL));
        for (Record record : additional) {
            if (record instanceof OPTRecord) {
                message.removeRecord(record, Section.ADDITIONAL);
            }
        }
        return message;
    }

}
