/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.authenticator

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ ArrayBuffer, Uint8Array }

trait Sha256PasswordPlugin extends AuthenticationPlugin:

  def password: Option[String]
  def publicKeyString: Option[String]

  private val crypto = js.Dynamic.global.require("crypto")

  override def name: String = "sha256_password"

  override def hashPassword(password: String, scramble: Array[Byte]): Array[Byte] =
    if password.isEmpty then Array[Byte]()
    else
      val hash1 = sha256(password.getBytes("UTF-8"))
      val hash2 = sha256(hash1)
      val hash3 = sha256(hash2 ++ scramble)

      hash1.zip(hash3).map { case (a, b) => (a ^ b).toByte }

  private def sha256(data: Array[Byte]): Array[Byte] =
    val hash = crypto.createHash("sha256")
    val buffer = new ArrayBuffer(data.length)
    val uint8Array = new Uint8Array(buffer)
    for (i <- data.indices) {
      uint8Array(i) = data(i)
    }
    hash.update(uint8Array)
    val digest = hash.digest()
    val result = new Array[Byte](digest.length.asInstanceOf[Int])
    for (i <- result.indices) {
      result(i) = digest(i).asInstanceOf[Byte]
    }
    result

object Sha256PasswordPlugin:

  def apply(_password: String): Sha256PasswordPlugin =
    new Sha256PasswordPlugin:
      override def password: Option[String] = Some(_password)
      override def publicKeyString: Option[String] = None

  def apply(_password: String, _publicKeyString: String): Sha256PasswordPlugin =
    new Sha256PasswordPlugin:
      override def password: Option[String] = Some(_password)
      override def publicKeyString: Option[String] = Some(_publicKeyString)
