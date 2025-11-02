package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.RIGID;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class HeaderCleaner implements PreHandler {

    private final DNSSecMode dnsSecMode;

    public HeaderCleaner(DNSSecMode dnsSecMode) {
        this.dnsSecMode = dnsSecMode;
    }

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        Header header = message.getHeader();
        header.unsetFlag(Flags.AD);
        header.unsetFlag(Flags.QR);
        if (RIGID.equals(dnsSecMode)) {
            header.unsetFlag(Flags.CD);
        }
        header.setFlag(Flags.RD);
        return message;
    }

}
