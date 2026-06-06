package com.allanvital.dnsao.dns.processor.pre.handler;
import com.allanvital.dnsao.infra.log.Log;

import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.util.List;


/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RequestLogger implements PreHandler {


    @Override
    public Message prepare(Message message) throws PreHandlerException {
        if (Log.DNS.isTraceEnabled()) {
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
            Log.DNS.trace("REQ flags -> Question={} AD={}, CD={}, DO={}", questionName, ad, cd, doFlag);
        }
        return message;
    }

}
