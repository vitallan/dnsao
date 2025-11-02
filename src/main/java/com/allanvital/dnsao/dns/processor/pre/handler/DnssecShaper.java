package com.allanvital.dnsao.dns.processor.pre.handler;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import com.allanvital.dnsao.exc.PreHandlerException;
import org.xbill.DNS.*;

import java.util.LinkedList;
import java.util.List;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.*;
import static org.xbill.DNS.EDNSOption.Code.PADDING;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DnssecShaper implements PreHandler {

    private static final int SIMPLE_CAP_BYTES = 96;

    private final DNSSecMode dnsSecMode;

    public DnssecShaper(DNSSecMode dnsSecMode) {
        this.dnsSecMode = dnsSecMode;
    }

    @Override
    public Message prepare(Message message) throws PreHandlerException {
        final int xrcode = 0;
        final int version = 0;
        final int flags = getFlag();
        List<EDNSOption> options = new LinkedList<>();
        OPTRecord oldOpt = message.getOPT();
        if (oldOpt != null) {
            options.addAll(oldOpt.getOptions());
            message.removeRecord(oldOpt, Section.ADDITIONAL);
        }
        addPadding(options, message);

        OPTRecord opt = new OPTRecord(DEFAULT_UDP_PAYLOAD, xrcode, version, flags, options);
        message.addRecord(opt, Section.ADDITIONAL);
        return message;
    }

    private void addPadding(List<EDNSOption> options, Message message) {
        if (OFF.equals(dnsSecMode)) {
            return;
        }
        int baseLen = message.toWire().length;
        int existingOptionsBytes = 0;
        for (EDNSOption opt : options) {
            if (opt == null) {
                continue; //better safe than sorry
            }
            int dataLen = opt.toWire().length;
            existingOptionsBytes += dataLen;
        }
        int sizeWithPaddingHeader = baseLen + OPT_FIXED_OVERHEAD + existingOptionsBytes + EDNS_OPTION_OVERHEAD;
        int desiredPadBytes = 0;
        if (SIMPLE.equals(dnsSecMode)) {
            int nextBoundary = ((sizeWithPaddingHeader + DEFAULT_BLOCK_SIZE - 1) / DEFAULT_BLOCK_SIZE) * DEFAULT_BLOCK_SIZE;
            int blockPad = nextBoundary - sizeWithPaddingHeader;
            desiredPadBytes = Math.min(blockPad, SIMPLE_CAP_BYTES);
        } else if (RIGID.equals(dnsSecMode)) {
            int fullPad = DEFAULT_UDP_PAYLOAD - sizeWithPaddingHeader;
            desiredPadBytes = Math.max(0, fullPad);
        } else {
            //not covered dnssec mode
            return;
        }
        int finalSize = sizeWithPaddingHeader + desiredPadBytes;
        if (finalSize > DEFAULT_UDP_PAYLOAD) {
            desiredPadBytes = DEFAULT_UDP_PAYLOAD - sizeWithPaddingHeader;
            if (desiredPadBytes < 0) {
                desiredPadBytes = 0;
            }
        }
        if (desiredPadBytes > 0) {
            byte[] paddingData = new byte[desiredPadBytes];
            EDNSOption paddingOption = new GenericEDNSOption(PADDING, paddingData);
            options.add(paddingOption);
        }
    }

    private int getFlag() {
        if (OFF.equals(dnsSecMode)) {
            return 0;
        }
        return ExtendedFlags.DO;
    }

}
