package com.gothesun.licensecore.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lawrence zhu
 * @date 2021-08-03
 */
public final class MachineUtils {
  private static final Logger log = LoggerFactory.getLogger(MachineUtils.class);

  private MachineUtils() {}

  protected static Set<String> getMacAddresses() {
    Set<String> macAddressSet = new HashSet<>();
    Enumeration<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface ni = networkInterfaces.nextElement();
        byte[] hardwareAddress = ni.getHardwareAddress();
        if (hardwareAddress != null) {
          String[] hexadecimalFormat = new String[hardwareAddress.length];
          for (int i = 0; i < hardwareAddress.length; i++) {
            hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
          }
          macAddressSet.add(String.join("-", hexadecimalFormat));
        }
      }
    } catch (SocketException e) {
      log.error("fail to get mac addresses", e);
    }

    return macAddressSet;
  }

  protected static String getIpAddress() {
    try (final DatagramSocket socket = new DatagramSocket()) {
      socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
      return socket.getLocalAddress().getHostAddress();
    } catch (SocketException | UnknownHostException e) {
      log.error("fail to get ip address", e);
    }

    return "";
  }
}
