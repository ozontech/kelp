package ru.ozon.ideplugin.kelp.colorPreviews

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.paint.RectanglePainter
import com.intellij.ui.scale.JBUIScale.scale
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.ImageUtil
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * Adapted version of [this](https://github.com/JetBrains/intellij-community/blob/00e97f8fe510d8beb6daab1592bc76ae715d67d2/platform/util/ui/src/com/intellij/util/ui/ColorsIcon.java)
 */
class RoundedColorsIcon : ColorIcon {
    private val myColors: Array<Color>
    private val cornerRadius: Int

    constructor(size: Int, cornerRadius: Int, vararg colors: Color) : super(size, size, Gray.TRANSPARENT, false) {
        myColors = ArrayUtil.reverseArray(colors)
        this.cornerRadius = cornerRadius
    }

    protected constructor(icon: RoundedColorsIcon) : super(icon) {
        myColors = icon.myColors
        cornerRadius = icon.cornerRadius
    }

    override fun copy(): RoundedColorsIcon = RoundedColorsIcon(this)

    override fun paintIcon(component: Component, g: Graphics, x: Int, y: Int) {
        val g2d = g.create() as Graphics2D
        val config = GraphicsUtil.setupAAPainting(g2d)
        try {
            val w = iconWidth
            val h = iconHeight
            if (myColors.size == 2) {
                g2d.clip = RoundRectangle2D.Float(
                    x.toFloat(),
                    y.toFloat(),
                    w.toFloat(),
                    h.toFloat(),
                    cornerRadius.toFloat(),
                    cornerRadius.toFloat()
                )
                g2d.paint = getPaint(myColors[0])
                g2d.fillPolygon(intArrayOf(x, x + w, x), intArrayOf(y, y, y + h), 3)
                g2d.paint = getPaint(myColors[1])
                g2d.fillPolygon(intArrayOf(x + w, x + w, x), intArrayOf(y, y + h, y + h), 3)
            } else {
                for (i in myColors.indices) {
                    g2d.paint = getPaint(myColors[i])
                    RectanglePainter.FILL.paint(
                        g2d,
                        if (i % 2 == 0) x else x + w / 2 + 1,
                        if (i < 2) y else y + h / 2 + 1,
                        w / 2 - 1,
                        h / 2 - 1, null
                    )
                    if (i == 3) break
                }
            }
        } catch (e: Exception) {
            g2d.dispose()
        } finally {
            config.restore()
        }
    }

    private fun getPaint(color: Color?): Paint = if (color == null || color.alpha == 0) CHESS else color

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false

        val icon = other as RoundedColorsIcon

        if (iconWidth != icon.iconWidth) return false
        if (iconHeight != icon.iconHeight) return false
        return myColors.contentEquals(icon.myColors)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + myColors.contentHashCode()
        return result
    }

    companion object {
        private val SQUARE_SIZE = scale(6)
        private val CHESS_IMAGE = ImageUtil.createImage(SQUARE_SIZE, SQUARE_SIZE, BufferedImage.TYPE_INT_RGB)
        private val CHESS: TexturePaint

        init {
            val graphics = CHESS_IMAGE.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            graphics.color = JBColor.LIGHT_GRAY
            graphics.fillRect(0, 0, SQUARE_SIZE + 1, SQUARE_SIZE + 1)
            graphics.color = JBColor.GRAY
            graphics.fillRect(0, 0, SQUARE_SIZE / 2, SQUARE_SIZE / 2)
            graphics.fillRect(SQUARE_SIZE / 2, SQUARE_SIZE / 2, SQUARE_SIZE / 2, SQUARE_SIZE / 2)
            graphics.dispose()
            CHESS = TexturePaint(CHESS_IMAGE, Rectangle(0, 0, SQUARE_SIZE, SQUARE_SIZE))
        }
    }
}