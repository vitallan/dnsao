package com.allanvital.dnsao.dns.recursive;

import org.xbill.DNS.Name;

interface RecursiveLookup {

    StepResponse resolve(Name qname, int qtype);
}
