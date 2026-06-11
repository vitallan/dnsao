package com.allanvital.dnsao.dns.recursive;

import com.allanvital.dnsao.infra.log.Log;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Master;
import org.xbill.DNS.Name;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Allan Vital (https://allanvital.com)
 */
public class RootHintsProvider {

    public static final String DEFAULT_ROOT_HINTS_URL = "https://www.internic.net/domain/named.root";

    private static final List<NameServerAddress> EMBEDDED_ROOT_SERVERS = List.of(
            new NameServerAddress("198.41.0.4"),
            new NameServerAddress("199.9.14.201"),
            new NameServerAddress("192.33.4.12"),
            new NameServerAddress("199.7.91.13"),
            new NameServerAddress("192.203.230.10"),
            new NameServerAddress("192.5.5.241"),
            new NameServerAddress("192.112.36.4"),
            new NameServerAddress("198.97.190.53"),
            new NameServerAddress("192.36.148.17"),
            new NameServerAddress("192.58.128.30"),
            new NameServerAddress("193.0.14.129"),
            new NameServerAddress("199.7.83.42"),
            new NameServerAddress("202.12.27.33")
    );

    private final String rootHintsUrl;
    private final RootHintsFileReader rootHintsFileReader;
    private List<NameServerAddress> rootServers = EMBEDDED_ROOT_SERVERS;
    private boolean initialized;

    public RootHintsProvider() {
        this(null, new DownloadRootHintsFileReader());
    }

    public RootHintsProvider(String rootHintsUrl) {
        this(rootHintsUrl, new DownloadRootHintsFileReader());
    }

    public RootHintsProvider(String rootHintsUrl, RootHintsFileReader rootHintsFileReader) {
        this.rootHintsUrl = rootHintsUrl;
        this.rootHintsFileReader = rootHintsFileReader;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        String url = resolveRootHintsUrl();
        List<NameServerAddress> downloadedRootServers = loadDownloadedRootServers(url);
        if (downloadedRootServers != null) {
            rootServers = downloadedRootServers;
            initialized = true;
            return;
        }

        List<NameServerAddress> cachedRootServers = loadCachedRootServers(url);
        if (cachedRootServers != null) {
            rootServers = cachedRootServers;
            initialized = true;
            return;
        }

        Log.INFRA.warn("failed to download root hints from {} and no valid cache was available", url);
        Log.INFRA.warn("falling back to embedded bootstrap root hints; recursive mode may be using stale root server data");
        rootServers = EMBEDDED_ROOT_SERVERS;
        initialized = true;
    }

    public List<NameServerAddress> getRootServers() {
        return rootServers;
    }

    private String resolveRootHintsUrl() {
        if (rootHintsUrl == null || rootHintsUrl.isBlank()) {
            return DEFAULT_ROOT_HINTS_URL;
        }
        return rootHintsUrl;
    }

    private List<NameServerAddress> loadDownloadedRootServers(String url) {
        try {
            Path downloadedFile = rootHintsFileReader.readDownloaded(url);
            List<NameServerAddress> parsedRootServers = parseRootHints(downloadedFile);
            rootHintsFileReader.persistCache(url, downloadedFile);
            Log.INFRA.info("loaded {} root hint addresses from {}", parsedRootServers.size(), url);
            return parsedRootServers;
        } catch (IOException | InterruptedException e) {
            Log.INFRA.warn("failed to refresh root hints from {}. Error was {}", url, e.getMessage());
            return null;
        }
    }

    private List<NameServerAddress> loadCachedRootServers(String url) {
        try {
            Optional<Path> cachedFile = rootHintsFileReader.readCached(url);
            if (cachedFile.isEmpty()) {
                return null;
            }

            List<NameServerAddress> parsedRootServers = parseRootHints(cachedFile.get());
            Log.INFRA.warn("using cached root hints for {}", url);
            return parsedRootServers;
        } catch (IOException e) {
            Log.INFRA.warn("failed to load cached root hints for {}. Error was {}", url, e.getMessage());
            return null;
        }
    }

    private List<NameServerAddress> parseRootHints(Path rootHintsPath) throws IOException {
        Set<Name> rootNameservers = new LinkedHashSet<>();
        Map<Name, List<NameServerAddress>> addressesByNameserver = new LinkedHashMap<>();

        try (Master master = new Master(rootHintsPath.toString())) {
            Record record;
            while ((record = master.nextRecord()) != null) {
                if (record instanceof NSRecord nsRecord && Name.root.equals(record.getName())) {
                    rootNameservers.add(nsRecord.getTarget());
                    continue;
                }
                if (record instanceof ARecord aRecord) {
                    addNameserverAddress(addressesByNameserver, aRecord.getName(), aRecord.getAddress().getHostAddress());
                    continue;
                }
                if (record instanceof AAAARecord aaaaRecord) {
                    addNameserverAddress(addressesByNameserver, aaaaRecord.getName(), aaaaRecord.getAddress().getHostAddress());
                }
            }
        }

        validateRootHints(rootNameservers, addressesByNameserver, rootHintsPath);
        return buildRootServerList(rootNameservers, addressesByNameserver);
    }

    private void addNameserverAddress(Map<Name, List<NameServerAddress>> addressesByNameserver, Name nameserver, String ip) {
        addressesByNameserver.computeIfAbsent(nameserver, ignored -> new ArrayList<>()).add(new NameServerAddress(ip));
    }

    private void validateRootHints(Set<Name> rootNameservers,
                                   Map<Name, List<NameServerAddress>> addressesByNameserver,
                                   Path rootHintsPath) throws IOException {
        if (rootNameservers.isEmpty()) {
            throw new IOException("root hints file " + rootHintsPath + " does not contain root NS records");
        }
        for (Name nameserver : rootNameservers) {
            List<NameServerAddress> addresses = addressesByNameserver.get(nameserver);
            if (addresses == null || addresses.isEmpty()) {
                throw new IOException("root hints file " + rootHintsPath + " is missing glue for " + nameserver);
            }
        }
    }

    private List<NameServerAddress> buildRootServerList(Set<Name> rootNameservers,
                                                        Map<Name, List<NameServerAddress>> addressesByNameserver) throws IOException {
        List<NameServerAddress> rootServerList = new ArrayList<>();
        Set<String> seenIps = new LinkedHashSet<>();

        for (Name nameserver : rootNameservers) {
            for (NameServerAddress nameserverAddress : addressesByNameserver.get(nameserver)) {
                if (seenIps.add(nameserverAddress.ip())) {
                    rootServerList.add(nameserverAddress);
                }
            }
        }

        if (rootServerList.isEmpty()) {
            throw new IOException("root hints did not yield any usable root server addresses");
        }

        return List.copyOf(rootServerList);
    }

}
