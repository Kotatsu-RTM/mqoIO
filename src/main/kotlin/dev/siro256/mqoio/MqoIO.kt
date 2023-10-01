package dev.siro256.mqoio

import dev.siro256.modelio.Model
import dev.siro256.modelio.ModelIO

object MqoIO : ModelIO {
    override val extension: Array<String>
        get() = TODO("Not yet implemented")

    override fun parse(byteArray: ByteArray): Result<Model> {
        TODO("Not yet implemented")
    }

    override fun export(model: Model): Result<ByteArray> {
        TODO("Not yet implemented")
    }
}
