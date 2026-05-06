package com.eottabom.letmecode.example.redis;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;

public class DnsCacheInspector {

    public void inspect(String host) {
        printSecurityProperty("networkaddress.cache.ttl");
        printSecurityProperty("networkaddress.cache.negative.ttl");
        resolve(host);
    }

    private void printSecurityProperty(String name) {
        String value = Security.getProperty(name);
        System.out.printf("%s=%s%n", name, value == null ? "<not set>" : value);
    }

    private void resolve(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            String resolvedAddresses = Arrays.stream(addresses)
                    .map(InetAddress::getHostAddress)
                    .toList()
                    .toString();

            System.out.printf("resolved host=%s addresses=%s%n", host, resolvedAddresses);
        } catch (UnknownHostException exception) {
            System.out.printf("failed host=%s exception=%s%n", host, exception);
        }
    }
}
