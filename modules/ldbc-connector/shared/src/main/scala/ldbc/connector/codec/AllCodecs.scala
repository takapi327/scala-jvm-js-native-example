/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

trait AllCodecs extends BooleanCodec, NumericCodecs, TextCodecs, TemporalCodecs

object all extends AllCodecs
