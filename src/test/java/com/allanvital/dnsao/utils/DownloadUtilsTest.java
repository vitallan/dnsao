package com.allanvital.dnsao.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class DownloadUtilsTest {

    @Test
    public void testFileUtilsFilename() {
        Assertions.assertEquals(
                "raw.githubusercontent.com_StevenBlack_hosts_master_hosts",
                DownloadUtils.fileName(URI.create("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts")));

        Assertions.assertEquals(
                "raw.githubusercontent.com_zangadoprojets_pi-hole-blocklist_main_Bets.txt",
                DownloadUtils.fileName(URI.create("https://raw.githubusercontent.com/zangadoprojets/pi-hole-blocklist/main/Bets.txt")));
    }

}