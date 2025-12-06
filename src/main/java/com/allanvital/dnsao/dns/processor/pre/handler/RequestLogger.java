package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;

import static com.allanvital.dnsao.infra.AppLoggers.DNS;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RequestLogger implements PreHandler {

    private static final Logger log = LoggerFactory.getLogger(DNS);

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        if (log.isTraceEnabled()) {
            Record question = message.getQuestion();
            String questionName = "";
            if (question != null) {
                Name name = question.getName();
                if (name != null) {
                    questionName = name.toString();
                }
            }
            boolean ad = message.getHeader().getFlag(Flags.AD);
            boolean cd = message.getHeader().getFlag(Flags.CD);
            List<org.xbill.DNS.Record> section = message.getSection(Section.ADDITIONAL);
            OPTRecord opt = null;
            for (Record r : section) {
                if (r instanceof OPTRecord) {
                    opt = (OPTRecord) r;
                    break;
                }
            }
            boolean doFlag = (opt != null) && ((opt.getFlags() & ExtendedFlags.DO) != 0);
            log.trace("REQ flags -> Question={} AD={}, CD={}, DO={}", questionName, ad, cd, doFlag);
        }
        return message;
    }

}
