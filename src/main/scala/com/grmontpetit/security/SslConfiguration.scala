/*
 * Copyright 2017 Gabriel Robitaille-Montpetit (grmontpetit@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.grmontpetit.security

import java.io.{File, FileInputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import com.typesafe.config.ConfigFactory
import spray.io.ServerSSLEngineProvider

trait SslConfiguration {

  implicit def sslContext: SSLContext = {
    val config = ConfigFactory.load()

    val ksPassword = config.getString("ssl_conf.ksPassword")
    val tsPassword = config.getString("ssl_conf.tsPassword")

    val ksPath= config.getString("ssl_conf.ksPath")
    val tsPath = config.getString("ssl_conf.tsPath")

    val ksLocation = openKeystoreFile(ksPath)
    val tsLocation = openKeystoreFile(tsPath)

    val ksStream = new FileInputStream(ksLocation)
    val tsStream = new FileInputStream(tsLocation)

    System.setProperty("javax.net.ssl.keyStore", ksLocation.getAbsolutePath)
    System.setProperty("javax.net.ssl.keyStorePassword", ksPassword)

    System.setProperty("javax.net.ssl.trustStore", tsLocation.getAbsolutePath)
    System.setProperty("javax.net.ssl.trustStorePassword", tsPassword)

    // Load keystore
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(ksStream, ksPassword.toCharArray)

    // Load trustore
    val trustStore = KeyStore.getInstance("JKS")
    trustStore.load(tsStream, tsPassword.toCharArray)

    // Create ks manager
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, ksPassword.toCharArray)
    val km = keyManagerFactory.getKeyManagers

    // Create ts manager
    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(trustStore)
    val tm = trustManagerFactory.getTrustManagers

    // Initialize SSLContext
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(km, tm, new SecureRandom)
    sslContext
  }

  /**
    * Try to retrieve a file at the root of the
    * project, if it doesn't work, check in
    * the resources folder.
    *
    * @param fileName The name of the file
    * @return The instance of the [[File]]
    */
  private def openKeystoreFile(fileName: String): File = {
    val file = new File(fileName)
    if (!file.exists()) {
      new File(getClass.getClassLoader.getResource(fileName).toURI)
    } else {
      file
    }
  }

  /**
    * Creayes a custom SSL Engine provider.
    * @return The [[ServerSSLEngineProvider]] instance.
    */
  implicit def sslEngineProvider = ServerSSLEngineProvider { engine =>
    engine.setWantClientAuth(false)
    engine.setEnableSessionCreation(true)
    engine.setUseClientMode(false)
    engine.setNeedClientAuth(false)
    engine.setEnabledProtocols(Array("TLSv1.2", "TLSv1.1", "TLSv1"))
    engine.setEnabledCipherSuites(getCustomCipherSuites())
    engine
  }

  /**
    * getCustomCipherSuites gives a list of cipher suites allowing the server to be compatible with most browser
    * and at the same time it provides maximum security
    *
    * @return a list of cipher suites suitable for TLS servers.
    */
  def getCustomCipherSuites(): Array[String] = {
    Array(
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
      "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
      "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
      "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
      "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
      "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
      "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
      "TLS_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
      "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
      "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
    )
  }
}
