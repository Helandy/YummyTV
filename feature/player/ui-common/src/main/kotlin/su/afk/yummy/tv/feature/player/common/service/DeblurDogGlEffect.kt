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
 * Режим «Резкость»: Anime4K v3.2 Deblur DoG (bloc97, MIT). Разница яркости с гауссовым размытием 5×5 усиливает
 * размытые контуры, а итог зажимается в минимум/максимум соседства 3×3 — поэтому без светлого ореола, который давала
 * обычная unsharp-маска. Работает на исходном разрешении.
 *
 * Порт под GLES 1.00 и 8-битные текстуры, чтобы режим работал и на GLES2-приставках: проход яркости из оригинала
 * слит с горизонтальным ядром (3 прохода вместо 4); все промежуточные значения лежат в 0..1.
 *
 * Шейдеры основаны на Anime4K: Copyright (c) 2019-2021 bloc97, MIT License
 * (https://github.com/bloc97/Anime4K/blob/master/LICENSE).
 */
@OptIn(UnstableApi::class)
internal class DeblurDogGlEffect : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        DeblurDogShaderProgram(useHdr)
}

@OptIn(UnstableApi::class)
private class DeblurDogShaderProgram(useHdr: Boolean) : BaseGlShaderProgram(useHdr, TEXTURE_POOL_CAPACITY) {
    private val kernelX = program(KERNEL_X_SHADER)
    private val kernelY = program(KERNEL_Y_SHADER)
    private val apply = program(APPLY_SHADER)

    private var width = 0
    private var height = 0
    private val textures = IntArray(2)
    private val framebuffers = IntArray(2)

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        if (width == inputWidth && height == inputHeight && textures[0] != 0) return Size(inputWidth, inputHeight)
        try {
            deleteBuffers()
            width = inputWidth
            height = inputHeight
            for (index in textures.indices) {
                textures[index] = GlUtil.createTexture(inputWidth, inputHeight, HIGH_PRECISION)
                framebuffers[index] = GlUtil.createFboForTexture(textures[index])
            }
            val texelSize = floatArrayOf(1f / inputWidth, 1f / inputHeight)
            listOf(kernelX, kernelY).forEach { it.setFloatsUniform("uTexelSize", texelSize) }
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
            draw(kernelX, framebuffers[0], "uTexSampler" to inputTexId)
            draw(kernelY, framebuffers[1], "uTexSampler" to textures[0])
            draw(apply, outputFramebuffer[0], "uTexSampler" to inputTexId, "uKernelSampler" to textures[1])
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    private fun draw(program: GlProgram, framebuffer: Int, vararg samplers: Pair<String, Int>) {
        GlUtil.focusFramebufferUsingCurrentContext(framebuffer, width, height)
        program.use()
        samplers.forEachIndexed { unit, (name, texture) -> program.setSamplerTexIdUniform(name, texture, unit) }
        program.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    override fun release() {
        super.release()
        try {
            deleteBuffers()
            listOf(kernelX, kernelY, apply).forEach(GlProgram::delete)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private fun deleteBuffers() {
        for (index in textures.indices) {
            if (framebuffers[index] != 0) GlUtil.deleteFbo(framebuffers[index])
            if (textures[index] != 0) GlUtil.deleteTexture(textures[index])
            framebuffers[index] = 0
            textures[index] = 0
        }
    }

    private companion object {
        const val TEXTURE_POOL_CAPACITY = 1

        /** 8-битных промежуточных текстур хватает (значения в 0..1), и они есть на GLES2. */
        const val HIGH_PRECISION = false

        fun program(fragmentShader: String): GlProgram = try {
            GlProgram(VERTEX_SHADER, fragmentShader).apply {
                setBufferAttribute(
                    "aFramePosition",
                    GlUtil.getNormalizedCoordinateBounds(),
                    GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
                )
            }
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }

        const val VERTEX_SHADER = """
attribute vec4 aFramePosition;
varying vec2 vTexSamplingCoord;
void main() {
  gl_Position = aFramePosition;
  vTexSamplingCoord = aFramePosition.xy * 0.5 + 0.5;
}
"""

        const val PRECISION = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
"""

        /** Яркость + горизонтальные гаусс 5 и минимум/максимум 3: (гаусс, min, max). */
        const val KERNEL_X_SHADER = PRECISION + """
uniform sampler2D uTexSampler;
uniform vec2 uTexelSize;
varying vec2 vTexSamplingCoord;
float luma(vec2 pos) {
  return dot(texture2D(uTexSampler, pos).rgb, vec3(0.299, 0.587, 0.114));
}
void main() {
  vec2 d = vec2(uTexelSize.x, 0.0);
  vec2 pos = vTexSamplingCoord;
  float a = luma(pos - d);
  float b = luma(pos);
  float c = luma(pos + d);
  float g = (luma(pos - (d + d)) + luma(pos + (d + d))) * 0.06136 + (a + c) * 0.24477 + b * 0.38774;
  gl_FragColor = vec4(g, min(min(a, b), c), max(max(a, b), c), 1.0);
}
"""

        /** Вертикальные гаусс 5 и минимум/максимум 3 поверх горизонтального прохода. */
        const val KERNEL_Y_SHADER = PRECISION + """
uniform sampler2D uTexSampler;
uniform vec2 uTexelSize;
varying vec2 vTexSamplingCoord;
void main() {
  vec2 d = vec2(0.0, uTexelSize.y);
  vec2 pos = vTexSamplingCoord;
  vec3 a = texture2D(uTexSampler, pos - d).xyz;
  vec3 b = texture2D(uTexSampler, pos).xyz;
  vec3 c = texture2D(uTexSampler, pos + d).xyz;
  float g = (texture2D(uTexSampler, pos - (d + d)).x + texture2D(uTexSampler, pos + (d + d)).x) * 0.06136 +
      (a.x + c.x) * 0.24477 + b.x * 0.38774;
  gl_FragColor = vec4(g, min(min(a.y, b.y), c.y), max(max(a.z, b.z), c.z), 1.0);
}
"""

        /**
         * Усиление разницы с размытием по кривой: слабые перепады (шум) не трогаются, уже резкие — почти тоже; итог
         * зажат в min/max соседства. Параметры оригинала: STRENGTH 0.6, BLUR_CURVE 0.6, BLUR_THRESHOLD 0.1,
         * NOISE_THRESHOLD 0.001.
         */
        const val APPLY_SHADER = PRECISION + """
uniform sampler2D uTexSampler;
uniform sampler2D uKernelSampler;
varying vec2 vTexSamplingCoord;
const float STRENGTH = 0.6;
const float BLUR_CURVE = 0.6;
const float BLUR_THRESHOLD = 0.1;
const float NOISE_THRESHOLD = 0.001;
void main() {
  vec4 color = texture2D(uTexSampler, vTexSamplingCoord);
  vec3 kernel = texture2D(uKernelSampler, vTexSamplingCoord).xyz;
  float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
  float c = (luma - kernel.x) * STRENGTH;
  float range = BLUR_THRESHOLD - NOISE_THRESHOLD;
  float ct = abs(c);
  if (ct > NOISE_THRESHOLD) {
    ct = pow((ct - NOISE_THRESHOLD) / range, BLUR_CURVE) * range + NOISE_THRESHOLD;
    ct = ct * sign(c);
  } else {
    ct = c;
  }
  float delta = clamp(ct + luma, kernel.y, kernel.z) - luma;
  gl_FragColor = vec4(clamp(color.rgb + delta, 0.0, 1.0), color.a);
}
"""
    }
}
