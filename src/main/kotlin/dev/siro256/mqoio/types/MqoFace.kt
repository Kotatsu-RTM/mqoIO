package dev.siro256.mqoio.types

import java.util.Optional

data class MqoFace(
    val v: Element.V,
    val m: Optional<Element.M>,
    val uv: Optional<Element.UV>,
) {
    sealed interface Element {
        data class V(val value: ArrayList<Int>) : Element
        data class M(val value: Int) : Element
        data class UV(val value: ArrayList<Pair<Float, Float>>) : Element
    }
}
