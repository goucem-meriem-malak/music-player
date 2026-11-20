package com.gmm.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Left/right balance for 16-bit stereo PCM. Other formats pass through untouched. */
class BalanceAudioProcessor : BaseAudioProcessor() {
    @Volatile private var leftGain = 1f
    @Volatile private var rightGain = 1f

    /** -1 = left only, 0 = centre, +1 = right only. */
    fun setBalance(balance: Float) {
        val b = balance.coerceIn(-1f, 1f)
        leftGain = if (b > 0) 1f - b else 1f
        rightGain = if (b < 0) 1f + b else 1f
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat =
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2) inputAudioFormat
        else AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val out = replaceOutputBuffer(remaining)
        inputBuffer.order(ByteOrder.nativeOrder())
        val l = leftGain
        val r = rightGain
        var i = inputBuffer.position()
        val end = inputBuffer.limit()
        while (i + 3 < end) {
            out.putShort((inputBuffer.getShort(i) * l).toInt().toShort())
            out.putShort((inputBuffer.getShort(i + 2) * r).toInt().toShort())
            i += 4
        }
        inputBuffer.position(end)
        out.flip()
    }
}
