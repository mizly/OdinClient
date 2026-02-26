package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import starred.skies.odin.utils.Skit

object Highlight : Module(
    name = "Highlight (C)",
    description = "Allows you to highlight selected entities.",
    category = Skit.CHEATS
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", true, desc = "Highlights starred dungeon mobs.")
    val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "The color of the highlight.")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val hideNonNames by BooleanSetting("Hide non-starred names", true, desc = "Hides names of entities that are not starred.")

    private val teammateClassGlow by BooleanSetting("Teammate Class Glow", true, desc = "Highlights dungeon teammates based on their class color.")

    private val dungeonMobSpawns = hashSetOf("Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer")
    // https://regex101.com/r/QQf502/1
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kM]?❤$")

    val highlightMap by MapSetting("highlightMap", mutableMapOf<String, Color>())

    private val customEntities = hashMapOf<Entity, Color>()
    private val starredEntities = hashSetOf<Entity>()

    init {
        OdinMod.logger.debug("Loaded ${highlightMap.entries.size}")

        on<TickEvent.End> {
            val world = mc.level ?: return@on

            val bool = highlightStar && DungeonUtils.inDungeons && !DungeonUtils.inBoss
            val bool0 = highlightMap.isNotEmpty()

            starredEntities.clear()
            customEntities.clear()

            if (!bool && !bool0) return@on

            for (stand in world.entitiesForRendering()) {
                if (stand !is ArmorStand || !stand.isAlive) continue

                val rawName = stand.displayName?.string?.noControlCodes?.takeIf { !it.equals("armor stand", true)} ?: continue
                val nameLower = rawName.lowercase()

                if (bool && dungeonMobSpawns.any(rawName::contains)) {
                    val starred = starredRegex.matches(rawName)
                    if (hideNonNames && stand.isInvisible && !starred) continue
                    if (starred) stand.fn()?.let(starredEntities::add)
                }

                if (bool0) {
                    val match = highlightMap.entries.firstOrNull { nameLower.contains(it.key) } ?: continue
                    stand.fn(true)?.let { customEntities[it] = match.value }
                }
            }
        }

        on<RenderEvent.Extract> {
            if (customEntities.isEmpty() && starredEntities.isEmpty()) return@on

            starredEntities.removeIf { !it.isAlive }
            customEntities.entries.removeIf { !it.key.isAlive }

            starredEntities.forEach {
                drawStyledBox(it.renderBoundingBox, color, renderStyle, depthCheck)
            }

            customEntities.forEach { (entity, color) ->
                drawStyledBox(entity.renderBoundingBox, color, renderStyle, depthCheck)
            }
        }

        on<WorldEvent.Load> {
            starredEntities.clear()
            customEntities.clear()
        }
    }

    private fun ArmorStand.fn(vis: Boolean = false): Entity? {
        val a = mc.level
            ?.getEntities(this, boundingBox.inflate(0.0, 1.0, 0.0)) { isValidEntity(it, vis) }
            ?.firstOrNull()

        if (a != null) return a

        return mc.level?.getEntity(id - 1)?.takeIf { isValidEntity(it, vis) }
    }

    private fun isValidEntity(entity: Entity, vis: Boolean = false): Boolean =
        when (entity) {
            is ArmorStand -> false
            is WitherBoss -> false
            is Player -> entity.uuid.version() == 2 && entity != mc.player
            else -> entity is EnderMan || (vis || !entity.isInvisible)
        }

    @JvmStatic
    fun getTeammateColor(entity: Entity): Int? {
        if (!enabled || !teammateClassGlow || !DungeonUtils.inDungeons || entity !is Player) return null
        return DungeonUtils.dungeonTeammates.find { it.name == entity.name?.string }?.clazz?.color?.rgba
    }
}