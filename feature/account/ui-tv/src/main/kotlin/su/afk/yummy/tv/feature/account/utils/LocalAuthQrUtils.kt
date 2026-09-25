package su.afk.yummy.tv.feature.account.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Матрица QR в минимальном размере: один модуль — одна ячейка, масштабирует уже Canvas.
 * Поле в один модуль — светлую рамку до «тихой зоны» дорисовывает подложка.
 */
internal fun encodeQrMatrix(content: String): BitMatrix = QRCodeWriter().encode(
    content,
    BarcodeFormat.QR_CODE,
    0,
    0,
    mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to Charsets.UTF_8.name(),
    ),
)
