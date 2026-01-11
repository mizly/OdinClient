package starred.skies.odin.events

import com.odtheking.odin.events.core.Event
import com.odtheking.odin.features.impl.floor7.terminalhandler.TerminalHandler

data class TerminalUpdateEvent(val terminal: TerminalHandler) : Event()