package dev.siro256.mqoio.types

import dev.siro256.mqoio.MqoIOException

enum class Axis {
    X, Y, Z;

    companion object {
        fun fromInt(number: Int): Result<Axis> =
            when (number) {
                1 -> Result.success(X)
                2 -> Result.success(Y)
                4 -> Result.success(Z)
                else -> Result.failure(MqoIOException.FileFormatException("Axis $number isn't correct."))
            }
    }
}
