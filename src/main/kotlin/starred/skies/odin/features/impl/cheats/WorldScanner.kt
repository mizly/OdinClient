package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawStyledBox
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import starred.skies.odin.utils.Skit
import java.util.concurrent.ConcurrentHashMap

object WorldScanner : Module(
    name = "World Scanner",
    description = "Scans and highlights structures in Crystal Hollows",
    category = Skit.CHEATS
) {
    private val scanCrystals by BooleanSetting("Scan Crystals", true, desc = "Scans for crystal waypoints")
    private val scanMobSpots by BooleanSetting("Scan Mob Spots", true, desc = "Scans for mob spawn locations")
    private val scanFairyGrottos by BooleanSetting("Scan Fairy Grottos", true, desc = "Scans for fairy grottos")
    private val scanDragonNest by BooleanSetting("Scan Dragon Nest", true, desc = "Scans for golden dragon nest")
    private val scanWormFishing by BooleanSetting("Scan Worm Fishing", false, desc = "Scans for worm fishing spots")
    private val renderStyle by SelectorSetting("Render Style", "Outline", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")
    private val sendCoordsInChat by BooleanSetting("Send Coords in Chat", true, desc = "Sends coordinates to chat when found")

    private val crystalWaypoints = ConcurrentHashMap<String, BlockPos>()
    private val mobSpotWaypoints = ConcurrentHashMap<String, BlockPos>()
    private val fairyGrottos = ConcurrentHashMap<BlockPos, Int>()
    private val dragonNests = ConcurrentHashMap<BlockPos, Int>()
    private val wormFishing = ConcurrentHashMap<BlockPos, Int>()
    private val scannedChunks = HashSet<Long>()

    private var cooldown = 100
    private var initialScan = false

    init {
        on<TickEvent.End> {
            if (cooldown > 0) {
                cooldown--
                return@on
            }

            if (cooldown == 0 && !initialScan) {
                initialScan = true
                performInitialScan()
            }
        }

        ClientChunkEvents.CHUNK_LOAD.register { _, chunk ->
            if (!enabled || cooldown > 0) return@register
            val chunkKey = getChunkKey(chunk)
            if (!scannedChunks.contains(chunkKey)) {
                handleChunkLoad(chunk)
                scannedChunks.add(chunkKey)
            }
        }

        on<WorldEvent.Load> {
            crystalWaypoints.clear()
            mobSpotWaypoints.clear()
            fairyGrottos.clear()
            dragonNests.clear()
            wormFishing.clear()
            scannedChunks.clear()
            cooldown = 80
            initialScan = false
        }

        on<RenderEvent.Extract> {
            if (cooldown > 0) return@on

            // Render crystal waypoints
            if (scanCrystals) {
                for ((name, pos) in crystalWaypoints) {
                    val color = getColorFromName(name)
                    val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
                    drawStyledBox(aabb, color, renderStyle, false)
                }
            }

            // Render mob spot waypoints
            if (scanMobSpots) {
                for ((name, pos) in mobSpotWaypoints) {
                    val color = getColorFromName(name)
                    val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
                    drawStyledBox(aabb, color, renderStyle, false)
                }
            }

            // Render fairy grottos
            if (scanFairyGrottos) {
                for (pos in fairyGrottos.keys) {
                    val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
                    drawStyledBox(aabb, Colors.MINECRAFT_LIGHT_PURPLE, renderStyle, false)
                }
            }

            // Render dragon nests
            if (scanDragonNest) {
                for (pos in dragonNests.keys) {
                    val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
                    drawStyledBox(aabb, Colors.MINECRAFT_GOLD, renderStyle, false)
                }
            }

            // Render worm fishing spots
            if (scanWormFishing) {
                for (pos in wormFishing.keys) {
                    val aabb = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos))
                    drawStyledBox(aabb, Colors.MINECRAFT_GOLD, renderStyle, false)
                }
            }
        }
    }

    private fun performInitialScan() {
        // Initial scan will happen gradually as chunks are accessed
        // This is to avoid potential issues with accessing chunk storage directly
        modMessage("World Scanner initialized - scanning chunks as they load...")
    }

    private fun handleChunkLoad(chunk: LevelChunk) {
        for (x in 0..15) {
            for (y in 0..170) {
                for (z in 0..15) {
                    val blockPos = BlockPos(chunk.pos.x * 16 + x, y, chunk.pos.z * 16 + z)
                    val blockState = getBlockState(chunk, x, y, z)

                    // Scan for crystals
                    if (scanCrystals) {
                        scanForCrystals(chunk, blockState, x, y, z, blockPos)
                    }

                    // Scan for mob spots
                    if (scanMobSpots) {
                        scanForMobSpots(chunk, blockState, x, y, z, blockPos)
                    }

                    // Scan for fairy grottos
                    if (scanFairyGrottos) {
                        scanForFairyGrottos(chunk, blockState, x, y, z, blockPos)
                    }

                    // Scan for dragon nests
                    if (scanDragonNest) {
                        scanForDragonNest(chunk, blockState, x, y, z, blockPos)
                    }

                    // Scan for worm fishing spots
                    if (scanWormFishing) {
                        scanForWormFishing(chunk, blockState, x, y, z, blockPos)
                    }
                }
            }
        }
    }

    private fun scanForCrystals(chunk: LevelChunk, blockState: BlockState, x: Int, y: Int, z: Int, blockPos: BlockPos) {
        // King crystal - Topaz (Yellow/Gold stained glass)
        if (blockState.block == Blocks.YELLOW_STAINED_GLASS) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.KING)) {
                addCrystalWaypoint("§6King", blockPos.offset(0, -1, 0))
            }
        }
        // Queen crystal - Amethyst (Magenta stained glass)
        else if (blockState.block == Blocks.MAGENTA_STAINED_GLASS) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.QUEEN)) {
                addCrystalWaypoint("§6Queen", blockPos.offset(0, -1, 0))
            }
        }
        // Divan crystal - Jade (Green stained glass)
        else if (blockState.block == Blocks.LIME_STAINED_GLASS) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.DIVAN)) {
                addCrystalWaypoint("§2Divan", blockPos.offset(0, -1, 0))
            }
        }
        // Temple crystal - Sapphire (Blue stained glass)
        else if (blockState.block == Blocks.BLUE_STAINED_GLASS) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.TEMPLE)) {
                addCrystalWaypoint("§5Temple", blockPos.offset(0, -1, 0))
            }
        }
        // City crystal - Amber (Orange stained glass)
        else if (blockState.block == Blocks.CYAN_STAINED_GLASS) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.CITY)) {
                addCrystalWaypoint("§bCity", blockPos.offset(0, -1, 0))
            }
        }
        // Bal crystal (Red/Black wool)
        else if (blockState.block == Blocks.RED_WOOL && y < 80) {
            if (checkPattern(chunk, x, y, z, CrystalPattern.BAL)) {
                addCrystalWaypoint("§6Bal", blockPos.offset(0, -1, 0))
            }
        }
    }

    private fun scanForFairyGrottos(chunk: LevelChunk, blockState: BlockState, x: Int, y: Int, z: Int, blockPos: BlockPos) {
        // Fairy grottos typically have a skull on top of a fence
        if (blockState.block == Blocks.SKELETON_SKULL || blockState.block == Blocks.SKELETON_WALL_SKULL) {
            val below = getBlockState(chunk, x, y - 1, z)
            if (below.block == Blocks.OAK_FENCE) {
                if (!fairyGrottos.containsKey(blockPos)) {
                    fairyGrottos[blockPos] = 0
                    if (sendCoordsInChat) {
                        modMessage("§dFairy Grotto §ffound at ${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                    }
                }
            }
        }
    }

    private fun scanForWormFishing(chunk: LevelChunk, blockState: BlockState, x: Int, y: Int, z: Int, blockPos: BlockPos) {
        val worldX = chunk.pos.x * 16 + x
        val worldZ = chunk.pos.z * 16 + z

        // Worm fishing spots are in specific coordinates (top right quadrant of CH)
        if ((worldX >= 564 && worldZ >= 513) || (worldX >= 513 && worldZ >= 564)) {
            if (y > 63 && blockState.block == Blocks.LAVA) {
                val above = getBlockState(chunk, x, y + 1, z)
                if (above.block != Blocks.LAVA) {
                    if (!wormFishing.containsKey(blockPos)) {
                        wormFishing[blockPos] = 0
                        if (sendCoordsInChat) {
                            modMessage("§6Worm Fishing §fspot found at ${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                        }
                    }
                }
            }
        }
    }

    private fun scanForMobSpots(chunk: LevelChunk, blockState: BlockState, x: Int, y: Int, z: Int, blockPos: BlockPos) {
        // All mob spot patterns from GumTuneClient

        // GOBLIN_HALL - Spruce planks base
        if (blockState.block == Blocks.SPRUCE_PLANKS && y < 160) {
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b3 = getBlockState(chunk, x, y + 3, z)
            val b6 = getBlockState(chunk, x, y + 6, z)
            val b7 = getBlockState(chunk, x, y + 7, z)
            val b10 = getBlockState(chunk, x, y + 10, z)
            val b11 = getBlockState(chunk, x, y + 11, z)
            val b13 = getBlockState(chunk, x, y + 13, z)

            if (b2.block == Blocks.SPRUCE_STAIRS && b3.block == Blocks.SPRUCE_STAIRS &&
                b6.block == Blocks.SPRUCE_STAIRS && b7.block == Blocks.SPRUCE_STAIRS &&
                b10.block == Blocks.SPRUCE_STAIRS && b11.block == Blocks.SPRUCE_STAIRS &&
                b13.block == Blocks.SPRUCE_PLANKS) {
                addMobSpotWaypoint("§6Goblin Hall", blockPos.offset(0, 7, 0))
                return
            }
        }

        // GOBLIN_RING - Oak fence base
        if (blockState.block == Blocks.OAK_FENCE && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b11 = getBlockState(chunk, x, y + 11, z)

            if ((b1.block == Blocks.SKELETON_SKULL || b1.block == Blocks.WITHER_SKELETON_SKULL) &&
                b11.block == Blocks.SPRUCE_PLANKS) {
                addMobSpotWaypoint("§6Goblin Ring", blockPos.offset(0, 11, 0))
                return
            }
        }

        // GRUNT_BRIDGE - Stone brick stairs
        if (blockState.block == Blocks.STONE_BRICK_STAIRS && y < 160) {
            val b5 = getBlockState(chunk, x, y + 5, z)
            val b6 = getBlockState(chunk, x, y + 6, z)
            val b8 = getBlockState(chunk, x, y + 8, z)
            val b9 = getBlockState(chunk, x, y + 9, z)
            val b13 = getBlockState(chunk, x, y + 13, z)
            val b14 = getBlockState(chunk, x, y + 14, z)

            if (b5.block == Blocks.STONE_BRICKS && b6.block == Blocks.STONE_BRICKS &&
                b8.block == Blocks.SMOOTH_STONE_SLAB && b9.block == Blocks.STONE_BRICKS &&
                b13.block == Blocks.STONE_BRICKS && b14.block == Blocks.SMOOTH_STONE_SLAB) {
                addMobSpotWaypoint("§bGrunt Bridge", blockPos.offset(0, -1, -45))
                return
            }
        }

        // CORLEONE_DOCK - Stone bricks pattern
        if (blockState.block == Blocks.STONE_BRICKS && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b3 = getBlockState(chunk, x, y + 3, z)
            val b24 = getBlockState(chunk, x, y + 24, z)
            val b25 = getBlockState(chunk, x, y + 25, z)
            val b26 = getBlockState(chunk, x, y + 26, z)
            val b27 = getBlockState(chunk, x, y + 27, z)

            if (b1.block == Blocks.STONE_BRICKS && b2.block == Blocks.STONE_BRICKS &&
                b3.block == Blocks.STONE_BRICKS && b24.block == Blocks.STONE_BRICKS &&
                b25.block == Blocks.STONE_BRICKS && b26.block == Blocks.FIRE &&
                b27.block == Blocks.STONE_BRICKS) {
                addMobSpotWaypoint("§bCorleone Dock", blockPos.offset(23, 11, 17))
                return
            }
        }

        // CORLEONE_HOLE - Stone slab bottom
        if (blockState.block == Blocks.SMOOTH_STONE_SLAB && y < 160) {
            val b14 = getBlockState(chunk, x, y + 14, z)
            val b15 = getBlockState(chunk, x, y + 15, z)
            val b17 = getBlockState(chunk, x, y + 17, z)
            val b18 = getBlockState(chunk, x, y + 18, z)

            if (b15.block == Blocks.SMOOTH_STONE && b18.block == Blocks.STONE_BRICKS) {
                addMobSpotWaypoint("§bCorleone Hole", blockPos.offset(0, -3, 34))
                return
            }
        }

        // YOG_BRIDGE - Stone bricks with rail
        if (blockState.block == Blocks.STONE_BRICKS && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b14 = getBlockState(chunk, x, y + 14, z)

            if (b1.block == Blocks.STONE_BRICK_STAIRS && b2.block == Blocks.STONE_BRICKS &&
                b14.block == Blocks.RAIL) {
                addMobSpotWaypoint("§6Yog Bridge", blockPos.offset(0, 15, 0))
                return
            }
        }

        // ODAWA - Jungle log with hay block
        if (blockState.block == Blocks.JUNGLE_LOG && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b9 = getBlockState(chunk, x, y + 9, z)
            val b10 = getBlockState(chunk, x, y + 10, z)

            if (b1.block == Blocks.SPRUCE_STAIRS && b9.block == Blocks.HAY_BLOCK &&
                b10.block == Blocks.YELLOW_TERRACOTTA) {
                addMobSpotWaypoint("§aOdawa", blockPos)
                return
            }
        }

        // KEY_GUARDIAN_SPIRAL - Jungle stairs
        if (blockState.block == Blocks.JUNGLE_STAIRS && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b7 = getBlockState(chunk, x, y + 7, z)
            val b8 = getBlockState(chunk, x, y + 8, z)

            if (b1.block == Blocks.JUNGLE_PLANKS && b2.block == Blocks.GLOWSTONE &&
                b7.block == Blocks.JUNGLE_STAIRS && b8.block == Blocks.STONE) {
                addMobSpotWaypoint("§aKey Guardian Spiral", blockPos)
                return
            }
        }

        // SLUDGE_BRIDGES - Jungle planks pattern
        if (blockState.block == Blocks.JUNGLE_PLANKS && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b3 = getBlockState(chunk, x, y + 3, z)
            val b4 = getBlockState(chunk, x, y + 4, z)
            val b9 = getBlockState(chunk, x, y + 9, z)
            val b10 = getBlockState(chunk, x, y + 10, z)

            if (b1.block == Blocks.JUNGLE_PLANKS && b2.block == Blocks.JUNGLE_PLANKS &&
                b3.block == Blocks.JUNGLE_STAIRS && b4.block == Blocks.JUNGLE_PLANKS &&
                b9.block == Blocks.GRANITE && b10.block == Blocks.GRANITE) {
                addMobSpotWaypoint("§aSludge Bridges", blockPos)
                return
            }
        }

        // GOBLIN_HOLE_CAMP - Netherrack with fence
        if (blockState.block == Blocks.NETHERRACK && y < 160) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b3 = getBlockState(chunk, x, y + 3, z)
            val b4 = getBlockState(chunk, x, y + 4, z)
            val b5 = getBlockState(chunk, x, y + 5, z)

            if (b1.block == Blocks.NETHERRACK && b2.block == Blocks.OAK_FENCE &&
                b3.block == Blocks.OAK_FENCE && b4.block == Blocks.OAK_LOG &&
                b5.block == Blocks.OAK_LOG) {
                addMobSpotWaypoint("§6Goblin Hole Camp", blockPos)
                return
            }
        }
    }

    private fun scanForDragonNest(chunk: LevelChunk, blockState: BlockState, x: Int, y: Int, z: Int, blockPos: BlockPos) {
        // Golden Dragon pattern: stone, red clay, red clay, red clay, skull, red wool
        if (blockState.block == Blocks.STONE && y > 0 && y < 150) {
            val b1 = getBlockState(chunk, x, y + 1, z)
            val b2 = getBlockState(chunk, x, y + 2, z)
            val b3 = getBlockState(chunk, x, y + 3, z)
            val b4 = getBlockState(chunk, x, y + 4, z)
            val b5 = getBlockState(chunk, x, y + 5, z)

            if (b1.block == Blocks.RED_TERRACOTTA &&
                b2.block == Blocks.RED_TERRACOTTA &&
                b3.block == Blocks.RED_TERRACOTTA &&
                (b4.block == Blocks.SKELETON_SKULL || b4.block == Blocks.WITHER_SKELETON_SKULL) &&
                b5.block == Blocks.RED_WOOL) {

                val nestPos = blockPos.offset(0, -3, 5)
                if (!dragonNests.containsKey(nestPos)) {
                    dragonNests[nestPos] = 0
                    if (sendCoordsInChat) {
                        modMessage("§6Dragon Nest §ffound at ${nestPos.x}, ${nestPos.y}, ${nestPos.z}")
                    }
                }
            }
        }
    }

    private fun addCrystalWaypoint(name: String, pos: BlockPos) {
        if (!crystalWaypoints.containsKey(name)) {
            crystalWaypoints[name] = pos
            if (sendCoordsInChat) {
                modMessage("$name §fcrystal found at ${pos.x}, ${pos.y}, ${pos.z}")
            }
        }
    }

    private fun addMobSpotWaypoint(name: String, pos: BlockPos) {
        if (!mobSpotWaypoints.containsKey(name)) {
            mobSpotWaypoints[name] = pos
            if (sendCoordsInChat) {
                modMessage("$name §fmob spot found at ${pos.x}, ${pos.y}, ${pos.z}")
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkPattern(chunk: LevelChunk, x: Int, y: Int, z: Int, pattern: CrystalPattern): Boolean {
        // Simple pattern check - can be expanded with more complex patterns
        // This is a simplified version and should be customized based on actual crystal structures
        return true
    }


    private fun getBlockState(chunk: LevelChunk, x: Int, y: Int, z: Int): BlockState {
        val sectionIndex = y shr 4
        val sections = chunk.sections

        if (sectionIndex < 0 || sectionIndex >= sections.size) {
            return Blocks.AIR.defaultBlockState()
        }

        val section = sections[sectionIndex]
        if (section == null || section.hasOnlyAir()) {
            return Blocks.AIR.defaultBlockState()
        }

        return section.getBlockState(x, y and 15, z)
    }

    private fun getChunkKey(chunk: LevelChunk): Long {
        return (chunk.pos.x.toLong() shl 32) or (chunk.pos.z.toLong() and 0xFFFFFFFFL)
    }

    private fun getColorFromName(name: String): Color {
        return when {
            name.contains("6") -> Colors.MINECRAFT_GOLD
            name.contains("2") -> Colors.MINECRAFT_GREEN
            name.contains("5") -> Colors.MINECRAFT_DARK_PURPLE
            name.contains("b") -> Colors.MINECRAFT_AQUA
            else -> Colors.WHITE
        }
    }

    private enum class CrystalPattern {
        KING, QUEEN, DIVAN, TEMPLE, CITY, BAL
    }
}
