package com.f4sitive.account.util;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class MacUtils {
    static String mac() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces())
                    .stream()
                    .filter(networkInterface -> {
                        try {
                            return !networkInterface.isLoopback() && !networkInterface.isPointToPoint() && !networkInterface.isVirtual() && networkInterface.isUp();
                        } catch (SocketException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(networkInterface -> {
                        try {
                            return networkInterface.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(hardwareAddress -> String.format("%0" + (hardwareAddress.length << 1) + "X", new BigInteger(1, hardwareAddress)))
                    .collect(Collectors.toSet())
                    .stream()
                    .sorted()
                    .collect(Collectors.joining("|", "|", "|"));
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
    }
}
