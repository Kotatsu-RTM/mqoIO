package dev.siro256.mqoio

sealed class MqoIOException : Exception {
    constructor(reason: String) : super(reason)
    constructor(throwable: Throwable) : super(throwable)

    class CharsetException(reason: String) : MqoIOException(reason)
    class FileFormatException(reason: String) : MqoIOException(reason)
    class IllegalStateException(reason: String) : MqoIOException(reason)
    class ParserException : MqoIOException {
        constructor(reason: String) : super(reason)
        constructor(throwable: Throwable) : super(throwable)
    }
    class UnsupportedFeatureException(reason: String) : MqoIOException(reason)
}
