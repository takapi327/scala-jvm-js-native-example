/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.*
import cats.syntax.all.*

import ldbc.connector.data.{ Type, Encoded }

trait Codec[A] extends Decoder[A], Encoder[A]:
  outer =>

  /** Forget this value is a `Codec` and treat it as an `Encoder`. */
  def asEncoder: Encoder[A] = this

  /** Forget this value is a `Codec` and treat it as a `Decoder`. */
  def asDecoder: Decoder[A] = this

  /** `Codec` is semigroupal: a pair of codecs make a codec for a pair. */
  def product[B](fb: Codec[B]): Codec[(A, B)] =
    new Codec[(A, B)]:
      private val pe = outer.asEncoder product fb.asEncoder
      private val pd = outer.asDecoder product fb.asDecoder

      override def encode(ab: (A, B)): List[Option[Encoded]] = pe.encode(ab)

      override def decode(offset: Int, ss: List[Option[String]]): Either[Decoder.Error, (A, B)] = pd.decode(offset, ss)

      override val types: List[Type] = outer.types ++ fb.types

  /** Shorthand for `product`. Note: consider using `a *: b *: c` instead of `a ~ b ~ c`. */
  @scala.annotation.targetName("productTo")
  def ~[B](fb: Codec[B]): Codec[Codec.~[A, B]] = product(fb)

object Codec:

  @scala.annotation.targetName("productTo")
  type ~[+A, +B] = (A, B)

  def apply[A](
    encode0: A => List[Option[String]],
    decode0: (Int, List[Option[String]]) => Either[Decoder.Error, A],
    oids0:   List[Type]
  ): Codec[A] =
    new Codec[A]:
      override def encode(a: A): List[Option[Encoded]] = encode0(a).map(_.map(Encoded(_)))

      override def decode(offset: Int, ss: List[Option[String]]): Either[Decoder.Error, A] = decode0(offset, ss)

      override val types: List[Type] = oids0

  def simple[A](encode: A => String, decode: String => Either[String, A], oid: Type): Codec[A] =
    apply(
      a => List(Some(encode(a))),
      (n, ss) =>
        ss match
          case Some(s) :: Nil => decode(s).leftMap(Decoder.Error(n, 1, _, oid))
          case None :: Nil    => Left(Decoder.Error(n, 1, "Unexpected NULL value in non-optional column.", oid))
          case _ => Left(Decoder.Error(n, 1, s"Expected one input value to decode, got ${ ss.length }.", oid))
      ,
      List(oid)
    )
