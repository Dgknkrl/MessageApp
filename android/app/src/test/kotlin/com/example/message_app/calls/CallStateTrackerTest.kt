package com.example.message_app.calls

import org.junit.Assert.*
import org.junit.Test

class CallStateTrackerTest {
    private val number = "+905551234567"

    @Test fun incomingAndAnswerAreEachEmittedOnce() {
        val ringing = CallStateTracker.next(CallStateTracker.Snapshot(), "RINGING", number)
        assertEquals(CallStateTracker.Event.INCOMING, ringing.event)
        val repeated = CallStateTracker.next(ringing.snapshot, "RINGING", null)
        assertNull(repeated.event)
        assertEquals(number, repeated.snapshot.number)
        val answered = CallStateTracker.next(repeated.snapshot, "OFFHOOK", null)
        assertEquals(CallStateTracker.Event.ANSWERED, answered.event)
        assertEquals(number, answered.snapshot.number)
        assertNull(CallStateTracker.next(answered.snapshot, "OFFHOOK", null).event)
        val ended = CallStateTracker.next(answered.snapshot, "IDLE", null)
        assertNull(ended.event)
        assertNull(ended.snapshot.number)
    }

    @Test fun unansweredCallClearsIdentityAndNextCallIsNotSuppressed() {
        val ringing = CallStateTracker.next(CallStateTracker.Snapshot(), "RINGING", number)
        val ended = CallStateTracker.next(ringing.snapshot, "IDLE", null)
        assertEquals(CallStateTracker.Event.UNANSWERED, ended.event)
        assertNull(ended.snapshot.number)
        assertNull(CallStateTracker.next(ended.snapshot, "IDLE", null).event)
        val next = CallStateTracker.next(ended.snapshot, "RINGING", "")
        assertEquals(CallStateTracker.Event.INCOMING, next.event)
        assertNull(next.snapshot.number)
    }

    @Test fun outgoingAndUnknownStatesDoNotNotify() {
        val outgoing = CallStateTracker.next(CallStateTracker.Snapshot(), "OFFHOOK", number)
        assertNull(outgoing.event)
        assertNull(outgoing.snapshot.number)
        assertNull(CallStateTracker.next(outgoing.snapshot, "IDLE", null).event)
        assertEquals(outgoing.snapshot, CallStateTracker.next(outgoing.snapshot, "INVALID", null).snapshot)
    }

    @Test fun persistedSnapshotCanContinueAfterProcessRestart() {
        val restored = CallStateTracker.Snapshot("RINGING", number)
        assertNull(CallStateTracker.next(restored, "RINGING", number).event)
        assertEquals(CallStateTracker.Event.ANSWERED, CallStateTracker.next(restored, "OFFHOOK", null).event)
        assertEquals(CallStateTracker.Event.UNANSWERED, CallStateTracker.next(restored, "IDLE", null).event)
    }
}
