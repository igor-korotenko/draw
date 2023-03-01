import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.presets.ORANGE_RED
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.edges.LumaSobel
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Rectangle
import kotlin.math.E
import kotlin.math.pow


fun main() {
    val size = 430.0

    application {
        val midiPlayer = AMidiPlayer().also { player ->
            player.load("data/midi/103013.mid")
            player.start()
        }
        configure {
            width = size.toInt()
            height = size.toInt()
        }
        program {
            val image = loadImage("data/images/lebedi.jpeg")
            val chromaticAberration = ChromaticAberration()
            val lumaSobel = LumaSobel()
            val filtered = colorBuffer(image.width, image.height)

            // keep a reference to the recorder so we can start it and stop it.
            val recorder = ScreenRecorder().apply {
                outputToVideo = false
            }
            extend(recorder)

            extend {
                lumaSobel.edgeColor = lumaSobel.edgeColor
                midiPlayer.notes.forEachIndexed { i, note ->
                    if (note.note < 67 && note.velocity > 67) {
                        chromaticAberration.aberrationFactor = E.pow(note.velocity / 20.0)
                    } else {
                        chromaticAberration.aberrationFactor = 1.0
                    }
                }
                lumaSobel.apply(image, filtered)
                chromaticAberration.apply(filtered, filtered)
                drawer.image(filtered)
            }

            val numPoints = 128
            extend {
                val top = ColorRGBa.ORANGE_RED.opacify(0.5)
                val bottom = ColorRGBa.WHITE
                drawer.fill = top
                drawer.stroke = null
                drawer.translate(0.0, size)
                drawer.shadeStyle = linearGradient(top, bottom)

                midiPlayer.notes.forEachIndexed { i, note ->
                    drawer.rectangles(List(numPoints) { num ->
                        val height = if (note.note == num) {
                            note.velocity.toDouble()
                        } else 0.0
                        val x = width * num / (numPoints - 1.0)
                        val y = -height * 1.5
                        Rectangle(x, y, (size/numPoints) * 10, height * 1.5)
                    })
                }
                midiPlayer.update()
            }

            keyboard.keyDown.listen {
                when {
                    it.key == KEY_ESCAPE -> program.application.exit()
                    it.name == "v" -> {
                        recorder.outputToVideo = !recorder.outputToVideo
                        println(if (recorder.outputToVideo) "Recording" else "Paused")
                    }
                }
            }
        }
    }
}

