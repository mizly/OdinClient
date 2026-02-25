package starred.skies.odin.utils

import com.odtheking.odin.OdinMod.mc
import starred.skies.odin.mixin.accessors.ClientPacketListenerAccessor

object TabListUtils {

    fun tabListContains(string: String): Boolean {
        return getTabList().any { cleanSB(it).contains(string, ignoreCase = true) }
    }

    fun getTabList(): List<String> {
        val connection = mc.player?.connection as? ClientPacketListenerAccessor ?: return emptyList()
        return connection.playerInfoMap.values.map {
            it.tabListDisplayName?.string ?: it.profile.name
        }
    }

    fun cleanSB(scoreboard: String): String {
        val cleaned = StringBuilder()
        val formatted = scoreboard.replace("§[0-9a-fk-or]".toRegex(), "")
        for (c in formatted) {
            if (c.code in 21..126) {
                cleaned.append(c)
            }
        }
        return cleaned.toString()
    }
}
