package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.SIMPLE;
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
        header.unsetFlag(Flags.QR);
        header.unsetFlag(Flags.AA);
        header.unsetFlag(Flags.RA);
        header.unsetFlag(Flags.TC);
        header.unsetFlag(Flags.AD);
        header.setRcode(Rcode.NOERROR);
        if (RIGID.equals(dnsSecMode) || SIMPLE.equals(dnsSecMode)) {
            header.setFlag(Flags.RD);
        }
        return message;
    }

}
