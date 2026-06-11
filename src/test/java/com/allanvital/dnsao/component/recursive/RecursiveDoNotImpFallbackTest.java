package com.allanvital.dnsao.component.recursive;

import org.xbill.DNS.Rcode;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RecursiveDoNotImpFallbackTest extends AbstractRecursiveDoDowngradeTest {

    @Override
    protected int downgradeTriggerRcode() {
        return Rcode.NOTIMP;
    }
}
