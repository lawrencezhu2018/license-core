/*
 *  * Copyright (c) 2021 gothesun.com. All Rights Reserved.
 */

package com.gothesun.licensecore.bean;

/**
 * 服务端的生成对象，包括license和私钥
 *
 * @author lawrence zhu
 * @date 2021-08-04
 */
public class Product {
  private License license;

  private String privateKey;

  public Product(License license, String privateKey) {
    this.license = license;
    this.privateKey = privateKey;
  }

  public License getLicense() {
    return license;
  }

  public String getPrivateKey() {
    return privateKey;
  }
}
