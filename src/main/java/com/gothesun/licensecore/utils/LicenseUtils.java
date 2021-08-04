/*
 *  * Copyright (c) 2021 gothesun.com. All Rights Reserved.
 */

package com.gothesun.licensecore.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gothesun.licensecore.bean.License;
import com.gothesun.licensecore.bean.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * License生成及校验入口
 *
 * @author lawrence zhu
 * @date 2021-08-03
 */
public final class LicenseUtils {
  private static final Logger log = LoggerFactory.getLogger(LicenseUtils.class);

  private LicenseUtils() {}

  /**
   * 生成指定路径下的license文件，并返回私钥
   *
   * @param application 应用名称
   * @param user 用户邮箱
   * @param project 用户项目
   * @param mac mac 用户服务器的mac地址
   * @param ip 用户服务器的ip地址
   * @param expirationAmount 有效期的数值
   * @param expirationUnit 有效期的单位
   * @param licensePath 授权文件路径
   * @return {@link String} 私钥
   * @throws Exception 异常
   */
  public static String generate(
      String application,
      String user,
      String project,
      String mac,
      String ip,
      long expirationAmount,
      TemporalUnit expirationUnit,
      Path licensePath)
      throws Exception {
    Product product =
        generate(application, user, project, mac, ip, expirationAmount, expirationUnit);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try (FileWriter writer = new FileWriter(licensePath.toString(), StandardCharsets.UTF_8)) {
      gson.toJson(product.getLicense(), writer);
    }

    return product.getPrivateKey();
  }

  /**
   * 生成license和私钥
   *
   * @param application 应用名称
   * @param user 用户邮箱
   * @param project 用户项目
   * @param mac 用户服务器mac地址
   * @param ip 用户服务器ip地址
   * @param expirationAmount 有效期的数值
   * @param expirationUnit 有效期的单位
   * @return {@link Product}
   * @throws Exception 异常
   */public static Product generate(
      String application,
      String user,
      String project,
      String mac,
      String ip,
      long expirationAmount,
      TemporalUnit expirationUnit)
      throws Exception {
    License license = new License();
    license.setApplication(application);
    license.setUser(user);
    license.setProject(project);
    license.setMac(mac);
    license.setIp(ip);
    license.setGenerated(LocalDateTime.now().toString());
    license.setExpiration(
        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            + Duration.of(expirationAmount, expirationUnit).toMillis());

    Map<String, Object> keyMap = RSAUtils.genKeyPair();
    String publicKey = RSAUtils.getPublicKey(keyMap);
    String privateKey = RSAUtils.getPrivateKey(keyMap);

    String message = getMessage(license);

    license.setPublicKey(publicKey);
    license.setSignature(RSAUtils.sign(message.getBytes(StandardCharsets.UTF_8), privateKey));

    return new Product(license, privateKey);
  }

  /**
   * 验证license文件
   *
   * @param licensePath license文件路径
   * @param currentTimestamp 当前时间戳 可选，若不填则使用本机时间验证
   * @return boolean
   * @throws IOException ioexception
   */
  public static boolean verify(Path licensePath, Long currentTimestamp) throws IOException {
    Gson gson = new Gson();
    License license =
        gson.fromJson(Files.newBufferedReader(licensePath, StandardCharsets.UTF_8), License.class);
    String message = getMessage(license);

    log.info("license info:{}", gson.toJson(license));

    try {
      return RSAUtils.verify(message.getBytes(StandardCharsets.UTF_8), license.getPublicKey(), license.getSignature())
          && Objects.equals(MachineUtils.getIpAddress(), license.getIp())
          && MachineUtils.getMacAddresses().contains(license.getMac())
          && license.getExpiration()
              > Optional.of(currentTimestamp).orElse(System.currentTimeMillis());

    } catch (Exception e) {
      log.error("fail to verify license", e);
      return false;
    }
  }

  private static String getMessage(License license) {
    return license.getVendor()
        + "|"
        + license.getApplication()
        + "|"
        + license.getSequence()
        + "|"
        + license.getUser()
        + "|"
        + license.getProject()
        + "|"
        + license.getMac()
        + "|"
        + license.getIp()
        + "|"
        + license.getGenerated()
        + "|"
        + license.getExpiration();
  }
}
