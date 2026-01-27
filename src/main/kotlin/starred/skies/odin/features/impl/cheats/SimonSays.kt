package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.rightClick
import kotlin.random.Random.Default.nextInt

object SimonSays : Module(
    name = "Simon Says Additions",
    description = "Additions for Simon Says!",
    category = Skit.CHEATS
) {
    private val startButton = BlockPos(110, 121, 91)
    private val autoStart by BooleanSetting("Auto start", false, desc = "Automatically starts the device when it can be started.")
    private val startClicks by NumberSetting("Start Clicks", 3, 1, 10, desc = "Amount of clicks to start the device.").withDependency { autoStart }
    private val startClickDelay by NumberSetting("Start Click Delay", 3, 1, 25, unit = "ticks", desc = "Delay between each start click.").withDependency { autoStart }

    init {
        on<ChatPacketEvent> {
            if (value == "[BOSS] Goldor: Who dares trespass into my domain?") s()
        }
    }

    private fun s() {
        val h = mc.hitResult?.takeIf { it.type == HitResult.Type.BLOCK } ?: return
        if ((h as? BlockHitResult)?.blockPos != startButton) return

        var i = 0
        while (i < startClicks) {
            schedule(i * startClickDelay + (2 + nextInt(2))) { rightClick() }
            i++
        }
    }
}