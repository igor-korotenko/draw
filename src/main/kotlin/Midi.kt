import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.sound.midi.*

data class Note(
    var channel: Int,
    var note: Int,
    var velocity: Int,
    var living: Int = 0,
    var dying: Int = 0
)

internal class AMidiPlayer : Receiver {
    // Concurrent, so it can be accessed by the render and the midi player
    // threads simultaneously without crashing
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