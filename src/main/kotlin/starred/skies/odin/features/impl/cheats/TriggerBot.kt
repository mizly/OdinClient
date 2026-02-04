package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.rightClick

object TriggerBot : Module(
    name = "TriggerBot (!!!)",
    description = "Triggers bots - bots trigger, Untested.",
    category = Skit.CHEATS
) {
    @Suppress("unused")
    private val UAYOR by BooleanSetting("Use at your own risk", false, desc = "TriggerBot is untested, use at your own risk!")
    private val crystalDropdown by DropdownSetting("Crystal Dropdown", false)
    private val crystal by BooleanSetting("Crystal", false, desc = "Automatically takes and places crystals.").withDependency { crystalDropdown }
    private val take by BooleanSetting("Take", true, desc = "Takes crystals.").withDependency { crystal && crystalDropdown }
    private val place by BooleanSetting("Place", true, desc = "Places crystals.").withDependency { crystal && crystalDropdown }

    private val secretDropdown by DropdownSetting("Secret Dropdown", false)
    private val secret by BooleanSetting("Secret", false, desc = "Automatically clicks secrets.").withDependency { secretDropdown }
    private val delay by NumberSetting("Delay", 200L, 0, 1000, unit = "ms", desc = "The delay between each click.").withDependency { secret && secretDropdown }

    private var click = 0L
    private var click0 = 0L
    private val clicked = mutableMapOf<BlockPos, Long>()

    init {
        on<TickEvent.Start> {
            if (!crystal) return@on
            if (!DungeonUtils.inBoss) return@on
            if (DungeonUtils.getF7Phase() != M7Phases.P1) return@on
            if (System.currentTimeMillis() - click < 500) return@on

            val hit = mc.hitResult ?: return@on
            if (hit.type != HitResult.Type.ENTITY) return@on

            val e = (hit as? EntityHitResult)?.entity ?: return@on
            val p = mc.player ?: return@on

            if (!take && !place) return@on
            if (take && e is EndCrystal) {
                rightClick()
                click = System.currentTimeMillis()
                return@on
            }

            if (!place) return@on
            if (e.name?.string?.noControlCodes != "Energy Crystal Missing") return@on
            if (p.mainHandItem?.displayName?.string?.noControlCodes != "Energy Crystal") return@on

            rightClick()
            click = System.currentTimeMillis()
        }

        on<TickEvent.Start> {
            if (!secret) return@on
            if (!DungeonUtils.inDungeons) return@on
            if (DungeonUtils.inBoss) return@on
            if (mc.screen != null) return@on
            if (System.currentTimeMillis() - click0 < delay) return@on
            if (DungeonUtils.currentRoomName.equalsOneOf("Water Board", "Three Weirdos")) return@on

            val hit = mc.hitResult ?: return@on
            if (hit.type != HitResult.Type.BLOCK) return@on

            val pos = (hit as? BlockHitResult)?.blockPos ?: return@on
            val state = world.getBlockState(pos) ?: return@on

            val n = System.currentTimeMillis()
            clicked.entries.removeIf { it.value + 1000L <= n }

            if (clicked.containsKey(pos)) return@on
            if (!DungeonUtils.isSecret(state, pos)) return@on

            rightClick()
            click0 = System.currentTimeMillis()
            clicked[pos] = n
        }

        on<WorldEvent.Load> {
            clicked.clear()
        }
    }
}