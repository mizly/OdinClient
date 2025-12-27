package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import org.lwjgl.glfw.GLFW
import starred.skies.odin.utils.Skit

object DungeonAbilities : Module(
    name = "Dungeon Abilities",
    description = "Automatically uses your ability in dungeons.",
    category = Skit.CHEATS
) {
    private val autoUlt by BooleanSetting("Auto Ult", false, desc = "Automatically uses your ultimate ability whenever needed.")
    private val abilityKeybind by KeybindSetting("Ability Keybind", GLFW.GLFW_KEY_UNKNOWN, desc = "Keybind to use your ability.").onPress {
        if (!DungeonUtils.inDungeons || !enabled) return@onPress
        dropItem(dropAll = true)
    }

    init {
        on<ChatPacketEvent> {
            if (!autoUlt) return@on

            val delay = when (value) {
                "⚠ Maxor is enraged! ⚠", "[BOSS] Goldor: You have done it, you destroyed the factory…" -> 1
                "[BOSS] Sadan: My giants! Unleashed!" -> 25
                else -> return@on
            }

            dropItem(delay = delay)
            modMessage("§aUsing ult!")
        }
    }

    private fun dropItem(dropAll: Boolean = false, delay: Int = 1) {
        schedule(delay) {
            mc.player?.drop(dropAll)
        }
    }
}