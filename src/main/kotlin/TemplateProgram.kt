import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.Random
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = loadImage("data/images/pm5544.png")
        val font = loadFont("data/fonts/default.otf", 64.0)

        extend {
            drawer.drawStyle.colorMatrix = tint(ColorRGBa.WHITE.shade(0.2))
            drawer.image(image)
        }

        val numPoints = 100
        val sineWaves = List(10) { SineWave(-5 rnd 5, 0 rnd PI * 2, 10 rnd 30) }

        extend {
            drawer.fill = ColorRGBa.WHITE.opacify(0.3)
            drawer.stroke = null
            drawer.rectangles(List(numPoints) { num ->
                val x = width * num / (numPoints - 1.0)
                val y = height * 0.5 + sineWaves.sumOf {
                    it.value(seconds, x * 0.01)
                }
                Rectangle(x, y, 10.0)
            })
        }
    }
}

data class SineWave(val freq: Double, val shift: Double, val amp: Double) {
    fun value(t: Double, x: Double) = amp * sin(t * freq + shift * x)
}

infix fun Number.rnd(max: Number) =
    Random.double(this.toDouble(), max.toDouble())