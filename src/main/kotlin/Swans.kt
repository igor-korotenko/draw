import org.openrndr.KEY_ESCAPE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.presets.GHOST_WHITE
import org.openrndr.extra.color.presets.ORANGE_RED
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.edges.LumaSobel
import org.openrndr.extra.olive.oliveProgram
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.shape.Rectangle
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.sound.midi.*
import javax.sound.midi.Receiver
import kotlin.math.*

/**
 *  This is a template for a live program.
 *
 *  It uses oliveProgram {} instead of program {}. All code inside the
 *  oliveProgram {} can be changed while the program is running.
 */

fun main() = application {
    val midiPlayer = AMidiPlayer().also { player ->
        player.load("data/midi/103013.mid")
        player.start()
    }
    configure {
        width = 380
        height = 380
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
            drawer.translate(0.0, 380.0)
            drawer.shadeStyle = linearGradient(top, bottom)

            midiPlayer.notes.forEachIndexed { i, note ->
                drawer.rectangles(List(numPoints) { num ->
                    val height = if (note.note == num) {
                        note.velocity.toDouble()
                    } else 0.0
                    val x = width * num / (numPoints - 1.0)
                    val y = -height * 1.5
                    Rectangle(x, y, (400.0/numPoints) * 10, height * 1.5)
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



data class Note(var channel: Int, var note: Int, var velocity: Int, var living: Int = 0, var dying: Int = 0)

internal class AMidiPlayer : Receiver {
    // Concurrent, so it can be accessed by the render and the midi player threads simultaneously without crashing
    private var midiData = ConcurrentHashMap<Int, Note>()
    private lateinit var sequencer: Sequencer

    fun load(path: String?) {
        val midiFile = File(path)
        sequencer = MidiSystem.getSequencer()
        sequencer.run {
            open()
            transmitter.receiver = this@AMidiPlayer
            sequence = MidiSystem.getSequence(midiFile)
        }
    }

    fun start() {
        sequencer.start()
    }

    fun update() {
        midiData.values.forEach { n ->
            if (n.dying > 0) {
                n.dying++
                if (n.dying > 10) {
                    val id = n.channel * 1000 + n.note
                    midiData.remove(id)
                }
            } else {
                n.living++
            }
        }
    }

    val bPM: Float
        get() = sequencer.tempoInBPM

    val notes: Collection<Note>
        get() = midiData.values

    // When I say "send" I mean "receive" :)
    override fun send(message: MidiMessage, t: Long) {
        if (message is ShortMessage) {
            val cmd = message.command
            if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF) {
                val channel = message.channel - 1
                val note = message.data1
                val velocity = message.data2
                val id = channel * 1000 + note
                if (cmd == ShortMessage.NOTE_ON && velocity > 0) {
                    midiData[id] = Note(channel, note, velocity)
                } else {
                    midiData[id]!!.dying++
                }
            }
        }
    }

    override fun close() {}
}

