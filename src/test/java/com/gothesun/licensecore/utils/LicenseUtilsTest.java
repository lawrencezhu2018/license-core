/*
 *  * Copyright (c) 2021 gothesun.com. All Rights Reserved.
 */

package com.gothesun.licensecore.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

/**
 * @author lawrence zhu
 * @date 2021-08-04
 */
class LicenseUtilsTest {
  private static final String LICENSE_PATH =
      "D:\\data\\code\\gothesun\\sdk\\license-core\\src\\test\\resources\\license.json";

  @Test
  void generate() throws Exception {
    Set<String> macs = MachineUtils.getMacAddresses();
    System.out.println(macs);
    String privateKey =
        LicenseUtils.generate(
            "gothesun.com",
            "letterlab",
            "license@gothesun.com",
            "license-core",
            "70-F1-1C-11-58-A",
            MachineUtils.getIpAddress(),
            1,
            ChronoUnit.YEARS,
            LICENSE_PATH);
    System.out.println(privateKey);
  }

  @Test
  void testGenerate() throws IOException, InterruptedException {
    URL url = new URL("https://www.baidu.com");
    URLConnection urlConnection = url.openConnection();
    urlConnection.connect();
    System.out.println(new Date(urlConnection.getDate()));
  }

  @Test
  void verify() throws IOException {
    Assertions.assertTrue(LicenseUtils.verify(Path.of(LICENSE_PATH), "gothesun.com","letterla",1L));
  }
}
