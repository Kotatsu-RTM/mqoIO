package dev.siro256.mqoio.types

import dev.siro256.mqoio.MqoIOException

enum class MirrorType {
    NONE,
    SEPARATE,
    CONNECT;

    companion object {
        fun fromInt(number: Int): Result<MirrorType> =
            when (number) {
                0 -> Result.success(NONE)
                1 -> Result.success(SEPARATE)
                2 -> Result.success(CONNECT)
                else -> Result.failure(
                    MqoIOException.FileFormatException("Mirror type $number isn't correct or supported.")
                )
            }
    }
}
