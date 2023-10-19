package dev.siro256.mqoio

import cc.ekblad.konbini.*
import dev.siro256.fastset.FastIndexSet
import dev.siro256.modelio.Model
import dev.siro256.modelio.ModelIO
import dev.siro256.mqoio.types.*
import org.mozilla.universalchardet.UniversalDetector
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.collections.ArrayList
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object MqoIO : ModelIO {
    private val mqoSeparator = regex(Regex("""[\t ]*"""))
    private val mqoChunkStart = string("{\r\n")
    private val mqoChunkEnd = parser {
        whitespace()
        char('}')
    }
    private val wholeLine = regex(Regex("""[^\r\n]*\r\n"""))

    private val pTop: Parser<Pair<Float, String?>> = parser {
        val version = pHeader()
        val codePage = tryParse { pCodePage() }

        version to codePage
    }
    private val pHeader: Parser<Float> = parser {
        string("Metasequoia Document\r\nFormat Text Ver ")
        val version = decimal().toFloat()
        string("\r\n")
        version
    }
    private val pCodePage: Parser<String> = parser {
        whitespace()
        regex(Regex("""(?i)CodePage"""))
        mqoSeparator()
        val codePage = oneOf(parser { string("utf8") }, parser { integer().toString() })
        mqoSeparator()
        string("\r\n")
        codePage
    }

    private val pMqo: Parser<Model> = parser {
        val chunks = many(parser { oneOf(pMaterial, pObject, pChunk) })

        val materials = chunks.firstOrNull { it is MqoMaterial }?.let { it as MqoMaterial }
        val objects = chunks.filterIsInstance<MqoObject>().map { mqoObject ->
            if (mqoObject.mirror.getOrNull()?.value == MirrorType.CONNECT)
                throw MqoIOException.UnsupportedFeatureException("Mirror type 2(connected mirror) isn't yet supported.")
            if (mqoObject.mirror.getOrNull()?.value == MirrorType.SEPARATE && !mqoObject.mirrorAxis.isPresent)
                throw MqoIOException.IllegalStateException("Object needs to be mirrored but mirror axis isn't present.")

            val vertices = mqoObject.vertex.value.map { Triple(it.first * 0.01f, it.second * 0.01f, it.third * 0.01f) }
            val faces = mutableListOf<Model.Face>()

            mqoObject.face.value.forEach { face ->
                val material = face.m.getOrNull()
                    ?.let {
                        materials?.materials?.getOrNull(it.value)
                            ?: throw MqoIOException.IllegalStateException("Object has material but not found.")
                    }

                val vertexIndices = face.v.value.reversed()
                if (vertexIndices.size != 3)
                    throw MqoIOException.UnsupportedFeatureException("mqoIO only supports triangulated face.")

                val uvs = face.uv.getOrNull()?.value?.reversed()
                if (uvs != null && uvs.size != 3)
                    throw MqoIOException.IllegalStateException("Number of uvs is less than number of vertices.")

                fun Pair<Float, Float>.toVector2f() = Model.Vector2f(first, second)

                fun Triple<Float, Float, Float>.toVector3f() = Model.Vector3f(first, second, third)

                val v1 = vertices.getOrNull(vertexIndices[0])?.toVector3f()
                    ?: throw MqoIOException.IllegalStateException("Missing vertex, index ${vertexIndices[0]}")
                val v2 = vertices.getOrNull(vertexIndices[1])?.toVector3f()
                    ?: throw MqoIOException.IllegalStateException("Missing vertex, index ${vertexIndices[1]}")
                val v3 = vertices.getOrNull(vertexIndices[2])?.toVector3f()
                    ?: throw MqoIOException.IllegalStateException("Missing vertex, index ${vertexIndices[2]}")
                val u1 = uvs?.get(0)?.toVector2f()
                val u2 = uvs?.get(1)?.toVector2f()
                val u3 = uvs?.get(2)?.toVector2f()

                faces.add(
                    Model.Face(
                        Optional.ofNullable(material),
                        calculateFaceNormal(v1, v2, v3),
                        Model.Vertex(v1, Optional.empty(), Optional.ofNullable(u1)),
                        Model.Vertex(v2, Optional.empty(), Optional.ofNullable(u2)),
                        Model.Vertex(v3, Optional.empty(), Optional.ofNullable(u3))
                    )
                )

                if (mqoObject.mirror.getOrNull()?.value == MirrorType.SEPARATE) {
                    val (mirroredV1, mirroredV2, mirroredV3) = when (mqoObject.mirrorAxis.get().value) {
                        Axis.X -> Triple(
                            Model.Vector3f(-v1.x, v1.y, v1.z),
                            Model.Vector3f(-v2.x, v2.y, v2.z),
                            Model.Vector3f(-v3.x, v3.y, v3.z)
                        )

                        Axis.Y -> Triple(
                            Model.Vector3f(v1.x, -v1.y, v1.z),
                            Model.Vector3f(v2.x, -v2.y, v2.z),
                            Model.Vector3f(v3.x, -v3.y, v3.z)
                        )

                        Axis.Z -> Triple(
                            Model.Vector3f(v1.x, v1.y, -v1.z),
                            Model.Vector3f(v2.x, v2.y, -v2.z),
                            Model.Vector3f(v3.x, v3.y, -v3.z)
                        )
                    }

                    faces.add(
                        Model.Face(
                            Optional.ofNullable(material),
                            calculateFaceNormal(mirroredV3, mirroredV2, mirroredV1),
                            Model.Vertex(mirroredV3, Optional.empty(), Optional.ofNullable(u3)),
                            Model.Vertex(mirroredV2, Optional.empty(), Optional.ofNullable(u2)),
                            Model.Vertex(mirroredV1, Optional.empty(), Optional.ofNullable(u1))
                        )
                    )
                }
            }

            Model.Object(mqoObject.name, faces)
        }

        return@parser Model(objects)
    }

    /**
     * https://www.khronos.org/opengl/wiki/Calculating_a_Surface_Normal
     */
    private fun calculateFaceNormal(p1: Model.Vector3f, p2: Model.Vector3f, p3: Model.Vector3f): Model.Vector3f {
        val u = p2.minus(p1)
        val v = p3.minus(p1)
        return u.cross(v).normalize()
    }

    private fun Model.Vector3f.minus(right: Model.Vector3f) =
        Model.Vector3f(
            x - right.x,
            y - right.y,
            z - right.z
        )

    private fun Model.Vector3f.cross(right: Model.Vector3f) =
        Model.Vector3f(
            y * right.z - z * right.y,
            z * right.x - x * right.z,
            x * right.y - y * right.x
        )

    private fun Model.Vector3f.length() =
        sqrt(x.toDouble().pow(2) + y.toDouble().pow(2) + z.toDouble().pow(2)).toFloat()

    private fun Model.Vector3f.normalize(): Model.Vector3f {
        val length = length()

        return Model.Vector3f(x / length, y / length, z / length)
    }

    private val pMaterial: Parser<MqoMaterial> = parser {
        bracket(
            parser {
                whitespace()
                regex(Regex("(?i)Material"))
                mqoSeparator()
                integer()
                mqoSeparator()
                mqoChunkStart()
            },
            mqoChunkEnd,
            parser { MqoMaterial(many(pMaterialData)) }
        )
    }
    private val pMaterialData: Parser<String> = parser {
        whitespace()
        val materialName = doubleQuotedString()
        wholeLine()
        materialName
    }

    private val pObject: Parser<MqoObject> = parser {
        atomically {
            val objectName = pObjectStart()
            val elements = many(parser {
                oneOf(
                    pObjectScale,
                    pObjectRotation,
                    pObjectTranslation,
                    pObjectFacet,
                    pObjectMirror,
                    pObjectMirrorAxis,
                    pObjectVertex,
                    pObjectFace,
                    pChunk
                )
            })
            mqoChunkEnd()

            MqoObject(
                objectName,
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.Scale }
                    ?.let { it as MqoObject.Element.Scale }),
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.Rotation }
                    ?.let { it as MqoObject.Element.Rotation }),
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.Translation }
                    ?.let { it as MqoObject.Element.Translation }),
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.Facet }
                    ?.let { it as MqoObject.Element.Facet }),
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.Mirror }
                    ?.let { it as MqoObject.Element.Mirror }),
                Optional.ofNullable(elements.firstOrNull { it is MqoObject.Element.MirrorAxis }
                    ?.let { it as MqoObject.Element.MirrorAxis }),
                elements.firstOrNull { it is MqoObject.Element.Vertex }?.let { it as MqoObject.Element.Vertex }
                    ?: throw MqoIOException.IllegalStateException("Object has no vertex chunk."),
                elements.firstOrNull { it is MqoObject.Element.Face }?.let { it as MqoObject.Element.Face }
                    ?: throw MqoIOException.IllegalStateException("Object has no face chunk.")
            )
        }
    }
    private val pObjectStart: Parser<String> = {
        whitespace()
        regex(Regex("(?i)Object"))
        mqoSeparator()
        val objectName = doubleQuotedString()
        mqoSeparator()
        mqoChunkStart()
        objectName
    }
    private val pObjectScale: Parser<MqoObject.Element.Scale> = parser {
        whitespace()
        regex(Regex("(?i)scale"))
        mqoSeparator()
        val x = decimal().toFloat()
        mqoSeparator()
        val y = decimal().toFloat()
        mqoSeparator()
        val z = decimal().toFloat()
        wholeLine()

        MqoObject.Element.Scale(x, y, z)
    }
    private val pObjectRotation: Parser<MqoObject.Element.Rotation> = parser {
        whitespace()
        regex(Regex("(?i)rotation"))
        mqoSeparator()
        val head = decimal().toFloat()
        mqoSeparator()
        val pitch = decimal().toFloat()
        mqoSeparator()
        val bank = decimal().toFloat()
        wholeLine()

        MqoObject.Element.Rotation(head, pitch, bank)
    }
    private val pObjectTranslation: Parser<MqoObject.Element.Translation> = parser {
        whitespace()
        regex(Regex("(?i)translation"))
        mqoSeparator()
        val x = decimal().toFloat()
        mqoSeparator()
        val y = decimal().toFloat()
        mqoSeparator()
        val z = decimal().toFloat()
        wholeLine()

        MqoObject.Element.Translation(x, y, z)
    }
    private val pObjectFacet: Parser<MqoObject.Element.Facet> = parser {
        whitespace()
        regex(Regex("(?i)facet"))
        mqoSeparator()
        val facet = decimal().toFloat()
        wholeLine()

        MqoObject.Element.Facet(facet)
    }
    private val pObjectMirror: Parser<MqoObject.Element.Mirror> = parser {
        whitespace()
        regex(Regex("(?i)mirror"))
        mqoSeparator()
        val mirror = integer().toInt()
        wholeLine()

        MqoObject.Element.Mirror(MirrorType.fromInt(mirror).getOrThrow())
    }
    private val pObjectMirrorAxis: Parser<MqoObject.Element.MirrorAxis> = parser {
        whitespace()
        regex(Regex("(?i)mirror_axis"))
        mqoSeparator()
        val mirrorAxis = integer().toInt()
        wholeLine()

        MqoObject.Element.MirrorAxis(Axis.fromInt(mirrorAxis).getOrThrow())
    }
    private val pObjectVertex: Parser<MqoObject.Element.Vertex> = bracket(
        parser {
            whitespace()
            regex(Regex("(?i)vertex"))
            mqoSeparator()
            integer()
            mqoSeparator()
            mqoChunkStart()
        },
        mqoChunkEnd,
        parser {
            val vertices = many(parser {
                whitespace()
                val x = decimal().toFloat()
                mqoSeparator()
                val y = decimal().toFloat()
                mqoSeparator()
                val z = decimal().toFloat()
                wholeLine()

                Triple(x, y, z)
            })

            MqoObject.Element.Vertex(ArrayList(vertices))
        }
    )
    private val pObjectFace: Parser<MqoObject.Element.Face> = bracket(
        parser {
            whitespace()
            regex(Regex("(?i)face"))
            mqoSeparator()
            integer()
            wholeLine()
        },
        mqoChunkEnd,
        parser {
            val faces = many(pObjectFaceElement)

            MqoObject.Element.Face(faces)
        }
    )
    private val pObjectFaceElement: Parser<MqoFace> = parser {
        whitespace()
        val elements = many(parser { oneOf(pObjectFaceV, pObjectFaceM, pObjectFaceUV, pObjectFaceOther) })
        wholeLine()

        if (elements.isEmpty()) fail("Doesn't match")

        MqoFace(
            elements.firstOrNull { it is MqoFace.Element.V }?.let { it as MqoFace.Element.V }
                ?: throw MqoIOException.IllegalStateException("Face has no vertex."),
            Optional.ofNullable(elements.firstOrNull { it is MqoFace.Element.M }?.let { it as MqoFace.Element.M }),
            Optional.ofNullable(elements.firstOrNull { it is MqoFace.Element.UV }?.let { it as MqoFace.Element.UV })
        )
    }
    private val pObjectFaceV: Parser<MqoFace.Element.V> = parser {
        mqoSeparator()
        regex(Regex("""(?i)V\("""))
        val indices = many(parser {
            mqoSeparator()
            integer().toInt()
        })
        mqoSeparator()
        char(')')

        MqoFace.Element.V(ArrayList(indices))
    }
    private val pObjectFaceM: Parser<MqoFace.Element.M> = parser {
        mqoSeparator()
        regex(Regex("""(?i)M\("""))
        val material = integer().toInt()
        mqoSeparator()
        char(')')

        MqoFace.Element.M(material)
    }
    private val pObjectFaceUV: Parser<MqoFace.Element.UV> = parser {
        mqoSeparator()
        regex(Regex("""(?i)UV\("""))
        val uvs = many(parser {
            mqoSeparator()
            val u = decimal().toFloat()
            mqoSeparator()
            val v = decimal().toFloat()

            u to v
        })
        mqoSeparator()
        char(')')

        MqoFace.Element.UV(ArrayList(uvs))
    }
    private val pObjectFaceOther: Parser<Unit> = parser {
        mqoSeparator()
        regex(Regex("""[^\r\n\t{} ]+"""))
    }

    private val pChunk: Parser<Unit> = parser {
        oneOf(pMultiLineChunk, pSingleLineChunk)
    }
    private val pSingleLineChunk: Parser<Unit> = parser {
        whitespace()
        regex(Regex("""[^\r\n{}]*\r\n"""))
    }
    private val pMultiLineChunk: Parser<Unit> = parser {
        bracket(
            parser {
                whitespace()
                regex(Regex("""[^\r\n{}]*\{\r\n"""))
            },
            parser {
                whitespace()
                char('}')
            },
            parser { many(pChunk) }
        )
    }

    override val extension: Array<String>
        get() = arrayOf("mqo", "mqoz")

    override fun parse(byteArray: ByteArray): Result<Model> {
        if (byteArray.isEmpty()) Result.success(Model(emptyList()))

        if (!isZipFormat(byteArray)) return parseMqo(byteArray)

        val byteArrayInputStream = ByteArrayInputStream(byteArray)
        val zipInputStream = ZipInputStream(byteArrayInputStream)

        @Suppress("ControlFlowWithEmptyBody")
        while (zipInputStream.nextEntry?.name?.endsWith(".mqo")?.equals(false)
                ?: return Result.failure(MqoIOException.FileFormatException("Corrupt MQOZ file"))
        );

        val mqo = zipInputStream.readBytes()

        zipInputStream.close()
        byteArrayInputStream.close()

        return parseMqo(mqo)
    }

    private fun isZipFormat(byteArray: ByteArray): Boolean {
        return byteArray.size >= 4 &&
                byteArray[0] == (0x50).toByte() &&
                byteArray[1] == (0x4b).toByte() &&
                byteArray[2] == (0x03).toByte() &&
                byteArray[3] == (0x04).toByte()
    }

    private fun parseMqo(mqo: ByteArray): Result<Model> {
        val charset = checkHeaderAndCharset(mqo).onFailure { return Result.failure(it) }.getOrThrow()
        val text = skipHeader(String(mqo, charset))

        return when (val result = pMqo.parse(text)) {
            is ParserResult.Error -> Result.failure(
                MqoIOException.ParserException(
                    """
                Failed to parse.
                    reason: ${result.reason}
                    at: line ${result.line}, colum ${result.column}
            """.trimIndent()
                )
            )

            is ParserResult.Ok -> Result.success(result.result)
        }
    }

    private fun checkHeaderAndCharset(byteArray: ByteArray): Result<Charset> {
        val destinationSize = min(64, byteArray.size)
        val top = ByteArray(destinationSize).apply { System.arraycopy(byteArray, 0, this, 0, destinationSize) }
            .decodeToString()

        val result = try {
            pTop.parse(top)
        } catch (exception: MqoIOException) {
            return Result.failure(MqoIOException.ParserException(exception))
        }

        when (result) {
            is ParserResult.Error -> return Result.failure(MqoIOException.ParserException(result.reason))
            is ParserResult.Ok -> {
                val parserOutput = result.result

                if (parserOutput.first !in 1.0f..<2.0f)
                    return Result.failure(MqoIOException.FileFormatException("Unsupported version of MQO"))

                if (parserOutput.first >= 1.2f) {
                    return when (val code = parserOutput.second) {
                        "utf8" -> Result.success(Charset.forName("UTF-8"))
                        "65001" -> Result.success(Charset.forName("UTF-8"))
                        "1200" -> Result.success(Charset.forName("UTF-16"))
                        "12000" -> Result.success(Charset.forName("UTF-32"))
                        "12001" -> Result.success(Charset.forName("UTF-32BE"))
                        "932" -> Result.success(Charset.forName("windows-31j"))
                        "20932" -> Result.success(Charset.forName("EUC-JP"))
                        else -> Result.failure(MqoIOException.CharsetException("Unsupported charset: $code"))
                    }
                }

                return Result.success(detectCharset(byteArray))
            }
        }
    }

    private fun detectCharset(byteArray: ByteArray): Charset {
        val detector = UniversalDetector()

        for (i in 0..<(byteArray.size / 16)) {
            if (detector.isDone) break
            detector.handleData(byteArray, i * 16, 16)
        }
        detector.dataEnd()

        return detector.detectedCharset?.let { Charset.forName(it) } ?: Charsets.UTF_8
    }

    private fun skipHeader(mqo: String): String {
        return if (mqo.contains("CodePage")) {
            mqo.substringAfter("CodePage").substringAfter('\n')
        } else {
            mqo.substringAfter('\n').substringAfter('\n')
        }
    }

    override fun export(model: Model): Result<ByteArray> {
        val builder = StringBuilder()

        builder.appendLine("Metasequoia Document")
            .appendLine("Format Text Ver 1.2")
            .appendLine("CodePage utf8")

        val materials = model.collectAllMaterials()
        if (materials.isNotEmpty()) {
            builder.appendLine("")
                .appendLine("Material ${materials.size} {")

            materials.forEach { builder.appendLine("    \"${it}\"") }

            builder.appendLine("}")
        }

        if (model.objects.isNotEmpty()) {
            model.objects.forEach { modelObject ->
                val optimizedObject =
                    modelObject.optimizeToExport(materials).onFailure { return Result.failure(it) }.getOrThrow()

                builder.appendLine("")
                    .appendLine("Object \"${optimizedObject.name}\" {")
                    .appendLine("    depth 0")

                val vertices = optimizedObject.vertex.value
                builder.appendLine("    vertex ${vertices.size} {")
                vertices.forEach { builder.appendLine("        ${it.first} ${it.second} ${it.third}") }
                builder.appendLine("    }")

                val faces = optimizedObject.face.value
                builder.appendLine("    face ${faces.size} {")
                faces.forEach { face ->
                    val faceVertices = face.v.value

                    builder.append("        ")
                        .append(faceVertices.size)
                        .append(" V(${faceVertices.joinToString(separator = " ")})")

                    face.m.ifPresent { builder.append(" M(${it.value})") }
                    face.uv.ifPresent { uv ->
                        builder.append("UV(${uv.value.joinToString(separator = " ") { "${it.first} ${it.second}" }})")
                    }

                    builder.appendLine("")
                }
                builder.appendLine("    }")

                builder.appendLine("}")
            }
        }

        builder.appendLine("Eof")

        return Result.success(builder.toString().toByteArray(Charsets.UTF_8))
    }

    private fun StringBuilder.appendLine(text: String) = append(text).append("\r\n")

    private fun Model.collectAllMaterials() =
        objects.flatMap { modelObject -> modelObject.faces.mapNotNull { it.material.getOrNull() } }.distinct()

    private fun Model.Object.optimizeToExport(materials: List<String>): Result<MqoObject> {
        val vertices = FastIndexSet<Triple<Float, Float, Float>>()
        val optimizedFaces = mutableListOf<MqoFace>()

        faces.forEach { face ->
            fun Model.Vector2f.toPair() = x to y

            fun Model.Vector3f.toTriple() = Triple(x, y, z)

            val faceVertices = arrayListOf(
                face.first.coordinate.toTriple(),
                face.second.coordinate.toTriple(),
                face.third.coordinate.toTriple()
            ).mapTo(ArrayList()) { Triple(it.first * 100.0f, it.second * 100.0f, it.third * 100.0f) }
                .reversed()

            val faceUVs = arrayListOf(
                face.first.uv.getOrNull()?.toPair(),
                face.second.uv.getOrNull()?.toPair(),
                face.third.uv.getOrNull()?.toPair()
            ).filterNotNullTo(arrayListOf())
                .reversed()

            if (faceUVs.isNotEmpty() && faceUVs.size != faceVertices.size)
                return Result.failure(
                    MqoIOException.IllegalStateException("Size of vertices and size of UVs are not same.")
                )

            vertices.addAll(faceVertices)

            optimizedFaces.add(
                MqoFace(
                    MqoFace.Element.V(ArrayList(faceVertices.mapTo(arrayListOf()) { vertices.indexOf(it) })),
                    Optional.ofNullable(face.material.getOrNull()?.let { MqoFace.Element.M(materials.indexOf(it)) }),
                    if (faceUVs.isEmpty()) Optional.empty() else Optional.of(MqoFace.Element.UV(ArrayList(faceUVs)))
            ))
        }

        return Result.success(MqoObject(
            name,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            MqoObject.Element.Vertex(ArrayList(vertices)),
            MqoObject.Element.Face(optimizedFaces)
            ))
    }
}
