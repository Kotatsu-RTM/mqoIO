package dev.siro256.mqoio.types

import java.util.Optional

data class MqoObject(
    val name: String,
    val scale: Optional<Element.Scale>,
    val rotation: Optional<Element.Rotation>,
    val translation: Optional<Element.Translation>,
    val facet: Optional<Element.Facet>,
    val mirror: Optional<Element.Mirror>,
    val mirrorAxis: Optional<Element.MirrorAxis>,
    val vertex: Element.Vertex,
    val face: Element.Face,
) {
    sealed interface Element {
        data class Scale(val x: Float, val y: Float, val z: Float) : Element
        data class Rotation(val head: Float, val pitch: Float, val bank: Float) : Element
        data class Translation(val x: Float, val y: Float, val z: Float) : Element
        data class Facet(val value: Float) : Element
        data class Mirror(val value: MirrorType) : Element
        data class MirrorAxis(val value: Axis) : Element
        data class Vertex(val value: ArrayList<Triple<Float, Float, Float>>) : Element
        data class Face(val value: List<MqoFace>)
    }
}
