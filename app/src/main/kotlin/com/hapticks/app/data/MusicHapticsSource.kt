package com.hapticks.app.data

enum class MusicHapticsSource {
    /** Capture audio playing on the device (requires MediaProjection, API 29+). */
    DEVICE,
    /** Capture audio from the microphone (surroundings). */
    SURROUNDINGS,
    /** Both in-device audio and microphone simultaneously. */
    BOTH;

    companion object {
        val Default = DEVICE
    }
}
