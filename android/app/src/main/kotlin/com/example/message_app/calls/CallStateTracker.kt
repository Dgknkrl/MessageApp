package com.example.message_app.calls

/** Repeated Android broadcasts must not produce duplicate messages. */
object CallStateTracker {
    data class Snapshot(val state: String = "IDLE", val number: String? = null)
    enum class Event { INCOMING, ANSWERED, UNANSWERED }
    data class Transition(val snapshot: Snapshot, val event: Event? = null)

    fun next(previous: Snapshot, state: String, number: String?): Transition {
        val caller = number?.takeIf { it.isNotBlank() }
        return when (state) {
            "RINGING" -> Transition(
                Snapshot(state, caller ?: previous.number.takeIf { previous.state == state }),
                Event.INCOMING.takeIf { previous.state != state },
            )
            "OFFHOOK" -> Transition(
                Snapshot(state, previous.number),
                Event.ANSWERED.takeIf { previous.state == "RINGING" },
            )
            "IDLE" -> Transition(
                Snapshot(),
                Event.UNANSWERED.takeIf { previous.state == "RINGING" },
            )
            else -> Transition(previous)
        }
    }
}
