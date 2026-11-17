package su.afk.yummy.tv.feature.player.common.service

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

/**
 * Режим «Качество»: официальный Anime4K Restore CNN S (bloc97, MIT) — четыре свёрточных прохода на исходном
 * разрешении, восстанавливают линии и гасят артефакты сжатия. Кадр не увеличивается: до экрана его растягивает
 * аппаратный скейлер вывода, как и без эффектов.
 *
 * Промежуточные результаты — две FP16-текстуры по очереди. Им нужен GLES3 с рендером в half float; на
 * GLES2-устройствах (старые Mali-450) программа не соберётся, и сервис плеера отключит режим сам.
 *
 * Перенос в Media3 GlEffect основан на коде Animeko (open-ani/animeko, AGPLv3): разбор проходов mpv-шейдера по
 * `//!DESC` и шаблоны `restore_pass_N.frag`, подменяющие mpv-макросы выборками текстур.
 */
@OptIn(UnstableApi::class)
internal class Anime4kRestoreGlEffect : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        Anime4kRestoreShaderProgram(context)
}

@OptIn(UnstableApi::class)
private class Anime4kRestoreShaderProgram(context: Context) : BaseGlShaderProgram(HIGH_PRECISION, TEXTURE_POOL_CAPACITY) {
    private val programs: List<GlProgram> = try {
        val vertexShader = context.readShader(VERTEX_SHADER)
        val passBodies = context.readShader(ANIME4K_SHADER).passBodies()
        check(passBodies.size == PASS_TEMPLATES.size) { "Anime4K Restore CNN S must contain four passes" }
        passBodies.mapIndexed { index, body ->
            val fragmentShader = context.readShader(PASS_TEMPLATES[index]).replace(PASS_BODY_PLACEHOLDER, body)
            GlProgram(vertexShader, fragmentShader).apply {
                setBufferAttribute(
                    "aFramePosition",
                    GlUtil.getNormalizedCoordinateBounds(),
                    GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
                )
            }
        }
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    private var width = 0
    private var height = 0
    private val intermediateTextures = IntArray(2)
    private val intermediateFramebuffers = IntArray(2)

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        if (width == inputWidth && height == inputHeight && intermediateTextures[0] != 0) {
            return Size(inputWidth, inputHeight)
        }
        try {
            deleteIntermediateBuffers()
            width = inputWidth
            height = inputHeight
            for (index in intermediateTextures.indices) {
                intermediateTextures[index] = GlUtil.createTexture(
                    inputWidth,
                    inputHeight,
                    HIGH_PRECISION,
                )
                intermediateFramebuffers[index] = GlUtil.createFboForTexture(intermediateTextures[index])
            }
            val texelSize = floatArrayOf(1f / inputWidth, 1f / inputHeight)
            programs.forEach { it.setFloatsUniform("uTexelSize", texelSize) }
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        // Выходной кадр базовый класс уже привязал: запоминаем его, чтобы вернуться после промежуточных проходов.
        val outputFramebuffer = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, outputFramebuffer, 0)
        try {
            drawPass(programs[0], inputTexId, intermediateFramebuffers[0])
            drawPass(programs[1], intermediateTextures[0], intermediateFramebuffers[1])
            drawPass(programs[2], intermediateTextures[1], intermediateFramebuffers[0])
            drawPass(programs[3], intermediateTextures[0], outputFramebuffer[0], originalTexId = inputTexId)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    private fun drawPass(program: GlProgram, inputTexId: Int, framebuffer: Int, originalTexId: Int? = null) {
        GlUtil.focusFramebufferUsingCurrentContext(framebuffer, width, height)
        program.use()
        program.setSamplerTexIdUniform("uTexSampler", inputTexId, INPUT_TEXTURE_UNIT)
        originalTexId?.let { program.setSamplerTexIdUniform("uOriginalSampler", it, ORIGINAL_TEXTURE_UNIT) }
        program.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        try {
            deleteIntermediateBuffers()
            programs.forEach(GlProgram::delete)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private fun deleteIntermediateBuffers() {
        for (index in intermediateTextures.indices) {
            if (intermediateFramebuffers[index] != 0) {
                GlUtil.deleteFbo(intermediateFramebuffers[index])
                intermediateFramebuffers[index] = 0
            }
            if (intermediateTextures[index] != 0) {
                GlUtil.deleteTexture(intermediateTextures[index])
                intermediateTextures[index] = 0
            }
        }
    }

    private companion object {
        /** Промежуточные и выходная текстуры — FP16: свёрточные признаки выходят за 0..1. */
        const val HIGH_PRECISION = true
        const val TEXTURE_POOL_CAPACITY = 1
        const val INPUT_TEXTURE_UNIT = 0
        const val ORIGINAL_TEXTURE_UNIT = 1
        const val SHADER_DIRECTORY = "video-enhancement"
        const val VERTEX_SHADER = "common.vert"
        const val ANIME4K_SHADER = "Anime4K_Restore_CNN_S.glsl"
        const val PASS_BODY_PLACEHOLDER = "{{PASS_BODY}}"
        val PASS_TEMPLATES = listOf(
            "restore_pass_1.frag",
            "restore_pass_2.frag",
            "restore_pass_3.frag",
            "restore_pass_4.frag",
        )

        fun Context.readShader(name: String): String =
            assets.open("$SHADER_DIRECTORY/$name").bufferedReader().use { it.readText() }

        /** Тела проходов mpv-шейдера: всё после строки `//!DESC` без служебных `//!`-директив. */
        fun String.passBodies(): List<String> =
            split(Regex("(?m)^//!DESC ")).drop(1).map { section ->
                section.lineSequence().drop(1).filterNot { it.startsWith("//!") }.joinToString("\n")
            }
    }
}
