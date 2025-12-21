package com.allanvital.dnsao.dns.processor.pre.handler.opt;

import com.allanvital.dnsao.conf.inner.DNSSecMode;
import org.xbill.DNS.EDNSOption;
import org.xbill.DNS.GenericEDNSOption;

import java.util.List;

import static com.allanvital.dnsao.conf.inner.DNSSecMode.*;
import static com.allanvital.dnsao.dns.processor.pre.handler.opt.Constants.*;
import static org.xbill.DNS.EDNSOption.Code.PADDING;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class OptPaddingProvider {

    private final DNSSecMode dnsSecMode;

    public OptPaddingProvider(DNSSecMode dnsSecMode) {
        this.dnsSecMode = dnsSecMode;
    }

    public void addPadding(List<EDNSOption> options, int messageBaseLen) {
        if (OFF.equals(dnsSecMode)) {
            return;
        }
        int existingOptionsBytes = 0;
        for (EDNSOption opt : options) {
            if (opt == null) {
                continue; //better safe than sorry
            }
            int dataLen = opt.toWire().length;
            existingOptionsBytes += dataLen;
        }
        int sizeWithPaddingHeader = messageBaseLen + OPT_FIXED_OVERHEAD + existingOptionsBytes + EDNS_OPTION_OVERHEAD;
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

}
