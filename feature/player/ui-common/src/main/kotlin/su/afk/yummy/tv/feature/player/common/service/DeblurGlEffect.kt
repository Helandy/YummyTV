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
 * Видеоэффект Deblur: резкость с порогом в один проход. Обычная unsharp-маска усиливает разницу пикселя с
 * размытым соседством везде, в том числе текстуру фона и шум сжатия. Здесь усиление включается только там, где
 * перепад яркости выше порога (линии, контуры), и плавно гаснет на слабых перепадах — фон не зернит.
 */
@OptIn(UnstableApi::class)
internal class DeblurGlEffect : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        DeblurShaderProgram(useHdr)
}

@OptIn(UnstableApi::class)
private class DeblurShaderProgram(useHdr: Boolean) : BaseGlShaderProgram(useHdr, 1) {
    private val glProgram: GlProgram = try {
        GlProgram(VERTEX_SHADER, FRAGMENT_SHADER).apply {
            setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
        }
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        glProgram.setFloatsUniform("uTexelSize", floatArrayOf(1f / inputWidth, 1f / inputHeight))
        glProgram.setFloatUniform("uStrength", STRENGTH)
        glProgram.setFloatsUniform("uThreshold", floatArrayOf(THRESHOLD_LOW, THRESHOLD_HIGH))
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    private companion object {
        const val VERTEX_SHADER = """
attribute vec4 aFramePosition;
varying vec2 vTexSamplingCoord;
void main() {
  gl_Position = aFramePosition;
  vTexSamplingCoord = aFramePosition.xy * 0.5 + 0.5;
}
"""
        /** Сила резкости на контурах (множитель разницы с размытым соседством). */
        const val STRENGTH = 1.5f

        /** Перепад яркости (0..1), ниже которого резкость не добавляется, и выше которого действует в полную силу. */
        const val THRESHOLD_LOW = 0.01f
        const val THRESHOLD_HIGH = 0.03f

        const val FRAGMENT_SHADER = """
precision mediump float;
uniform sampler2D uTexSampler;
uniform vec2 uTexelSize;
uniform float uStrength;
uniform vec2 uThreshold;
varying vec2 vTexSamplingCoord;
const vec3 LUMA = vec3(0.299, 0.587, 0.114);
void main() {
  vec4 c = texture2D(uTexSampler, vTexSamplingCoord);
  vec3 blur = 0.25 * (
      texture2D(uTexSampler, vTexSamplingCoord + vec2(uTexelSize.x, 0.0)).rgb +
      texture2D(uTexSampler, vTexSamplingCoord - vec2(uTexelSize.x, 0.0)).rgb +
      texture2D(uTexSampler, vTexSamplingCoord + vec2(0.0, uTexelSize.y)).rgb +
      texture2D(uTexSampler, vTexSamplingCoord - vec2(0.0, uTexelSize.y)).rgb);
  vec3 detail = c.rgb - blur;
  float gate = smoothstep(uThreshold.x, uThreshold.y, abs(dot(detail, LUMA)));
  gl_FragColor = vec4(clamp(c.rgb + detail * uStrength * gate, 0.0, 1.0), c.a);
}
"""
    }
}
