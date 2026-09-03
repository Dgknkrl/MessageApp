package com.example.message_app.calls

import android.content.Context
import com.example.message_app.contacts.ContactResolver
import com.example.message_app.data.AppPreferences
import com.example.message_app.telegram.TelegramSender

object CallStateStore {
    private const val FILE = "call_state"

    @Synchronized
    fun reset(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Synchronized
    fun handle(context: Context, state: String, number: String?, occurredAt: Long) {
        if (!AppPreferences.isEnabled(context) || !AppPreferences.hasTelegramSettings(context)) {
            reset(context)
            return
        }
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val previous = CallStateTracker.Snapshot(
            prefs.getString("state", "IDLE") ?: "IDLE",
            prefs.getString("number", null),
        )
        val transition = CallStateTracker.next(previous, state, number)
        val caller = if (state == "IDLE") previous.number else transition.snapshot.number
        val name = if (transition.event == CallStateTracker.Event.INCOMING) {
            ContactResolver.name(context, caller)
        } else prefs.getString("name", null)
        transition.event?.let {
            // Save before finishing the broadcast; HTTP uses a separate worker.
            TelegramSender.enqueueCall(context, caller, name, it, occurredAt)
        }
        prefs.edit()
            .putString("state", transition.snapshot.state)
            .putString("number", transition.snapshot.number)
            .putString("name", name.takeUnless { state == "IDLE" })
            .commit()
    }
}
