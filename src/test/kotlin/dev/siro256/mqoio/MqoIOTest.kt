package dev.siro256.mqoio

import dev.siro256.modelio.Model
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MqoIOTest {
    @Test
    fun testExtension() {
        assertContentEquals(arrayOf("mqo", "mqoz"), MqoIO.extension)
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    fun testParse(byteArray: SimpleToStringByteArray, model: Model) {
        assertEquals(model, MqoIO.parse(byteArray.byteArray).getOrThrow())
    }

    @ParameterizedTest
    @MethodSource("testDataProvider")
    fun testExport(byteArray: SimpleToStringByteArray, model: Model) {
        assertEquals(model, MqoIO.parse(MqoIO.export(model).getOrThrow()).getOrThrow())
    }

    companion object {
        private val case1File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case1.mqoz")!!
                .readBytes().wrap()
        private val case2File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case2.mqoz")!!
                .readBytes().wrap()
        private val case3File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case3.mqoz")!!
                .readBytes().wrap()
        private val case4File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case4.mqoz")!!
                .readBytes().wrap()
        private val case5File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case5.mqoz")!!
                .readBytes().wrap()
        private val case6File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case6.mqoz")!!
                .readBytes().wrap()
        private val case7File =
            this::class.java.classLoader.getResourceAsStream("dev/siro256/mqoio/MqoIOTest/case7.mqo")!!
                .readBytes().wrap()
        private val case1ExpectedModel =
            Model(emptyList())
        private val case2ExpectedModel =
            Model(
                listOf(
                    Model.Object("empty", emptyList()),
                    Model.Object(
                        "triangle",
                        listOf(
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, 0.0f, -1.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            )
                        )
                    )
                )
            )
        private val case3ExpectedModel =
            Model(
                listOf(
                    Model.Object(
                        "square",
                        listOf(
                            Model.Face(
                                Optional.empty(),
                                Model.Vector3f(0.0f, 0.0f, -1.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.empty()
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.empty()
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.empty()
                                )
                            ),
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, 0.0f, -1.0f),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 1.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                )
                            )
                        )
                    ),
                )
            )
        private val case4ExpectedModel =
            Model(
                listOf(
                    Model.Object(
                        "xAxisMirrored",
                        listOf(
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, 0.0f, 1.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            ),
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, 0.0f, 1.0f),
                                Model.Vertex(
                                    Model.Vector3f(-1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(-1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                )
                            )
                        )
                    ),
                    Model.Object(
                        "yAxisMirrored",
                        listOf(
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(1.0f, 0.0f, 0.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 1.0f, 1.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            ),
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(-1.0f, 0.0f, 0.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, -1.0f, 1.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, -1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                )
                            )
                        )
                    ),
                    Model.Object(
                        "zAxisMirrored",
                        listOf(
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, -1.0f, 0.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 1.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            ),
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, -1.0f, 0.0f),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, -1.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                )
                            )
                        )
                    ),
                )
            )
        private val case5AndCase6ExpectedModel =
            Model(
                listOf(
                    Model.Object(
                        "三角形",
                        listOf(
                            Model.Face(
                                Optional.of("マテリアル"),
                                Model.Vector3f(0.0f, 0.0f, 1.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            )
                        )
                    )
                )
            )
        private val case7ExpectedModel =
            Model(
                listOf(
                    Model.Object(
                        "triangle",
                        listOf(
                            Model.Face(
                                Optional.of("material"),
                                Model.Vector3f(0.0f, 0.0f, 1.0f),
                                Model.Vertex(
                                    Model.Vector3f(0.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(0.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 0.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 0.0f))
                                ),
                                Model.Vertex(
                                    Model.Vector3f(1.0f, 1.0f, 0.0f),
                                    Optional.empty(),
                                    Optional.of(Model.Vector2f(1.0f, 1.0f))
                                )
                            )
                        )
                    )
                )
            )

        /**
         * | objects # | triangles # | has material | has uv  | has mirrored face | char type | charset  | extension | What's the test for?                                  |
         * | :-------- | :---------- | :----------- | :------ | :---------------- | :-------- | :------- | :-------- | :---------------------------------------------------- |
         * | 0         | 0           | no           | no      | no                | ASCII     | UTF-8    | mqoz      | Empty model                                           |
         * | 2         | 0, 1        | no, yes      | no, yes | no                | ASCII     | UTF-8    | mqoz      | Empty object and non-empty object                     |
         * | 1         | 2           | no-yes       | no-yes  | no                | ASCII     | UTF-8    | mqoz      | Face without material and with material in one object |
         * | 3         | 2           | yes          | yes     | yes               | ASCII     | UTF-8    | mqoz      | Mirrored face                                         |
         * | 1         | 1           | yes          | yes     | no                | Japanese  | UTF-8    | mqoz      | Japanese model name and material name with UTF-8      |
         * | 1         | 1           | yes          | yes     | no                | Japanese  | ShiftJIS | mqoz      | Japanese model name and material name with ShiftJIS   |
         * | 1         | 1           | yes          | yes     | no                | ASCII     | UTF-8    | mqo       | Uncompressed format                                   |
         */
        @JvmStatic
        fun testDataProvider() =
            listOf(
                Arguments.of(case1File, case1ExpectedModel),
                Arguments.of(case2File, case2ExpectedModel),
                Arguments.of(case3File, case3ExpectedModel),
                Arguments.of(case4File, case4ExpectedModel),
                Arguments.of(case5File, case5AndCase6ExpectedModel),
                Arguments.of(case6File, case5AndCase6ExpectedModel),
                Arguments.of(case7File, case7ExpectedModel)
            )

        private fun ByteArray.wrap() = SimpleToStringByteArray(this)

        class SimpleToStringByteArray(val byteArray: ByteArray) {
            override fun toString() = "ByteArray(length=${byteArray.size})"
        }
    }
}
