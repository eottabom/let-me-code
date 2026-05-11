package com.eottabom.letmecode.example.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;

public class DnsCacheInspector {

    private static final Logger log = LoggerFactory.getLogger(DnsCacheInspector.class);

    public void inspect(String host) {
        printSecurityProperty("networkaddress.cache.ttl");
        printSecurityProperty("networkaddress.cache.negative.ttl");
        resolve(host);
    }

    private void printSecurityProperty(String name) {
        String value = Security.getProperty(name);
        log.info("{}={}", name, value == null ? "<not set>" : value);
    }

    private void resolve(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            String resolvedAddresses = Arrays.stream(addresses)
                    .map(InetAddress::getHostAddress)
                    .toList()
                    .toString();

            log.info("resolved host={} addresses={}", host, resolvedAddresses);
        } catch (UnknownHostException exception) {
            log.warn("failed host={} exception={}", host, exception.getMessage());
        }
    }
}
