/*
 *  * Copyright (c) 2021 gothesun.com. All Rights Reserved.
 */

package com.gothesun.licensecore.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

/**
 * @author lawrence zhu
 * @date 2021-08-04
 */
public final class CommonUtils {
  private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);

  private CommonUtils() {}

  /**
   * 简化的uuid
   *
   * @return 简化的uuid
   */
   public static String uuid() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  public static long getCurrentTimestamp() {
    try {
      URL url = new URL("https://www.baidu.com");
      URLConnection urlConnection = url.openConnection();
      urlConnection.connect();
      return urlConnection.getDate();
    } catch (IOException e) {
      log.warn("fail to connect baidu");
      return System.currentTimeMillis();
    }
  }
}
