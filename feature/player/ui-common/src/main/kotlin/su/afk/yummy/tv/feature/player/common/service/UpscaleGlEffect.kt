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
import kotlin.math.roundToInt

/**
 * Апскейл кадра до [targetHeight] бикубическим фильтром Catmull-Rom (16 выборок). Ставится после [DeblurGlEffect]:
 * без него SurfaceView растягивает кадр до экрана билинейно и смазывает добавленную резкость.
 * Если кадр не ниже целевой высоты — проходит как есть.
 */
@OptIn(UnstableApi::class)
internal class UpscaleGlEffect(private val targetHeight: Int) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        UpscaleShaderProgram(useHdr, targetHeight)
}

@OptIn(UnstableApi::class)
private class UpscaleShaderProgram(
    useHdr: Boolean,
    private val targetHeight: Int,
) : BaseGlShaderProgram(useHdr, 1) {
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
        glProgram.setFloatsUniform("uInputSize", floatArrayOf(inputWidth.toFloat(), inputHeight.toFloat()))
        if (inputHeight >= targetHeight) return Size(inputWidth, inputHeight)
        val outputWidth = (inputWidth.toFloat() * targetHeight / inputHeight / 2f).roundToInt() * 2
        return Size(outputWidth, targetHeight)
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

        // Выборки берутся в центрах текселей входа, веса Catmull-Rom считаются по дробной части позиции.
        const val FRAGMENT_SHADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform sampler2D uTexSampler;
uniform vec2 uInputSize;
varying vec2 vTexSamplingCoord;
vec4 weights(float t) {
  return vec4(
      t * (-0.5 + t * (1.0 - 0.5 * t)),
      1.0 + t * t * (-2.5 + 1.5 * t),
      t * (0.5 + t * (2.0 - 1.5 * t)),
      t * t * (-0.5 + 0.5 * t));
}
vec3 tap(vec2 texel) {
  return texture2D(uTexSampler, (texel + 0.5) / uInputSize).rgb;
}
vec3 row(vec2 base, float dy, vec4 wx) {
  return tap(base + vec2(-1.0, dy)) * wx.x + tap(base + vec2(0.0, dy)) * wx.y +
      tap(base + vec2(1.0, dy)) * wx.z + tap(base + vec2(2.0, dy)) * wx.w;
}
void main() {
  vec2 pos = vTexSamplingCoord * uInputSize - 0.5;
  vec2 base = floor(pos);
  vec2 f = pos - base;
  vec4 wx = weights(f.x);
  vec4 wy = weights(f.y);
  vec3 color = row(base, -1.0, wx) * wy.x + row(base, 0.0, wx) * wy.y +
      row(base, 1.0, wx) * wy.z + row(base, 2.0, wx) * wy.w;
  gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
"""
    }
}
