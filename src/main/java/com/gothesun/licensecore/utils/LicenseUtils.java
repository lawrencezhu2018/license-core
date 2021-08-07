/*
 *  * Copyright (c) 2021 gothesun.com. All Rights Reserved.
 */

package com.gothesun.licensecore.utils;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Preconditions;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
      String vendor,
      String application,
      String user,
      String project,
      String mac,
      String ip,
      long expirationAmount,
      TemporalUnit expirationUnit,
      String licensePath)
      throws Exception {
    Product product =
        generate(vendor, application, user, project, mac, ip, expirationAmount, expirationUnit);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try (FileWriter writer = new FileWriter(licensePath, StandardCharsets.UTF_8)) {
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
   */
  public static Product generate(
      String vendor,
      String application,
      String user,
      String project,
      String mac,
      String ip,
      long expirationAmount,
      TemporalUnit expirationUnit)
      throws Exception {
    String tip = "%s should not be blank";
    Preconditions.checkArgument(StrUtil.isNotBlank(vendor), tip, "vendor");
    Preconditions.checkArgument(StrUtil.isNotBlank(application), tip, "application");
    Preconditions.checkArgument(StrUtil.isNotBlank(user), tip, "user");
    Preconditions.checkArgument(StrUtil.isNotBlank(project), tip, "project");
    Preconditions.checkArgument(Validator.isMac(mac), "[%s] is not valid mac address", mac);
    Preconditions.checkArgument(
        Validator.isIpv4(ip) || Validator.isIpv6(ip), "[%s] is not valid ip address", ip);

    License license = new License();
    license.setVendor(vendor);
    license.setApplication(application);
    license.setSequence(CommonUtils.uuid());
    license.setUser(user);
    license.setProject(project);
    license.setMac(mac);
    license.setIp(ip);
    license.setGenerated(ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
    license.setExpiration(
        LocalDateTime.now()
            .plus(expirationAmount, expirationUnit)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli());

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
   * @param vendor 供应商
   * @param application 应用
   * @param currentTimestamp 当前时间戳 可选
   * @return boolean
   * @throws IOException ioexception
   */
  public static boolean verify(
      Path licensePath, String vendor, String application, Long currentTimestamp)
      throws IOException {
    String tip = "%s should not be blank";
    Preconditions.checkArgument(
        Files.isRegularFile(licensePath) && Files.isReadable(licensePath),
        "[%s] is not a readable license file",
        licensePath);
    Preconditions.checkArgument(StrUtil.isNotBlank(vendor), tip, "vendor");
    Preconditions.checkArgument(StrUtil.isNotBlank(application), tip, "application");
    Preconditions.checkArgument(
        Objects.isNull(currentTimestamp) || currentTimestamp > 0,
        "[%s] is not valid currentTimestamp",
        currentTimestamp);

    Gson gson = new Gson();
    License license =
        gson.fromJson(Files.newBufferedReader(licensePath, StandardCharsets.UTF_8), License.class);
    Preconditions.checkArgument(Objects.nonNull(license), tip,"license file");
    log.info("license info:{}", gson.toJson(license));

    String message = getMessage(license);

    try {
      return RSAUtils.verify(
              message.getBytes(StandardCharsets.UTF_8),
              license.getPublicKey(),
              license.getSignature())
          && Objects.equals(vendor, license.getVendor())
          && Objects.equals(application, license.getApplication())
          && Objects.equals(MachineUtils.getIpAddress(), license.getIp())
          && MachineUtils.getMacAddresses().contains(license.getMac())
          && license.getExpiration()
              > Optional.ofNullable(currentTimestamp).orElse(CommonUtils.getCurrentTimestamp());

    } catch (Exception e) {
      log.error("fail to verify license", e);
      return false;
    }
  }

  /**
   * 验证license文件
   *
   * @param licensePath license文件路径
   * @param vendor 供应商
   * @param application 应用
   * @return boolean 证书是否合法
   * @throws IOException ioexception
   */
  public static boolean verify(Path licensePath, String vendor, String application)
      throws IOException {
    return verify(licensePath, vendor, application, null);
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
