package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.dns.processor.pre.handler.opt.OptBasicInfoProvider;
import com.allanvital.dnsao.dns.processor.pre.handler.opt.OptFlagProvider;
import com.allanvital.dnsao.dns.processor.pre.handler.opt.OptPaddingProvider;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.EDNSOption;
import org.xbill.DNS.Message;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Section;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnsPrivacyShaper implements PreHandler {

    private final OptBasicInfoProvider basicInfoProvider;
    private final OptFlagProvider flagProvider;
    private final OptPaddingProvider paddingProvider;

    public DnsPrivacyShaper(DNSSecMode dnsSecMode) {
        this.basicInfoProvider = new OptBasicInfoProvider();
        this.flagProvider = new OptFlagProvider(dnsSecMode);
        this.paddingProvider = new OptPaddingProvider(dnsSecMode);
    }

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        List<EDNSOption> options = new LinkedList<>();
        OPTRecord oldOpt = message.getOPT();
        if (oldOpt != null) {
            options.addAll(oldOpt.getOptions());
            message.removeRecord(oldOpt, Section.ADDITIONAL);
        }

        int payloadSize = basicInfoProvider.getUdpPayloadSize();
        int xrcode = basicInfoProvider.getXrCode();
        int version = basicInfoProvider.getVersion();
        int flags = flagProvider.getFlags();
        paddingProvider.addPadding(options, message.toWire().length);

        OPTRecord opt = new OPTRecord(payloadSize, xrcode, version, flags, options);

        message.addRecord(opt, Section.ADDITIONAL);
        return message;
    }

}
