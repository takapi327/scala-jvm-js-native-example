/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scala.scalajs.js
import scala.scalajs.js.typedarray._

class MysqlNativePasswordPlugin extends AuthenticationPlugin:

  private val crypto = js.Dynamic.global.require("crypto")

  override def name: String = "mysql_native_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha1(password.getBytes("UTF-8"))
      val hash2 = sha1(hash1)
      val hash3 = sha1(scramble ++ hash2)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha1(data: Array[Byte]): Array[Byte] =
    val hash = crypto.createHash("sha1")
    val buffer = new ArrayBuffer(data.length)
    val uint8Array = new Uint8Array(buffer)
    for (i <- data.indices) {
      uint8Array(i) = (data(i) & 0xff).toByte // Ensure that the byte is treated as unsigned
    }
    hash.update(uint8Array)
    val digest = hash.digest("hex")
    val result = new Array[Byte](digest.length.asInstanceOf[Int] / 2)
    for (i <- result.indices) {
      result(i) = Integer.parseInt(digest.substring(2 * i, 2 * i + 2).asInstanceOf[String], 16).toByte
    }
    result
