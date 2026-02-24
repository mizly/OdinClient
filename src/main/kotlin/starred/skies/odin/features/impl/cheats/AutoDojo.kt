package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawStyledBox
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Skeleton
import net.minecraft.world.entity.monster.WitherSkeleton
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import starred.skies.odin.mixin.accessors.InventoryAccessor
import starred.skies.odin.utils.Skit
import starred.skies.odin.utils.RotationUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object AutoDojo : Module(
    name = "Auto Dojo",
    description = "Automatically completes Hypixel SkyBlock dojo tests",
    category = Skit.CHEATS
) {
    private val enableControl by BooleanSetting("Enable Control", true, desc = "Automatically aim at skeleton in Test of Control")
    private val controlPredictionTicks by NumberSetting("Control Prediction Ticks", 5.0, 1.0, 20.0, 1.0, desc = "How many ticks ahead to predict skeleton movement")
    private val enableMastery by BooleanSetting("Enable Mastery", true, desc = "Automatically shoot blocks in Test of Mastery")
    private val masteryShootDelay by NumberSetting("Mastery Shoot Delay (ms)", 600.0, 0.0, 2000.0, 50.0, desc = "Time remaining on yellow block before shooting")
    private val enableDiscipline by BooleanSetting("Enable Discipline", true, desc = "Automatically switch swords in Test of Discipline")
    private val renderStyle by SelectorSetting("Render Style", "Filled", listOf("Filled", "Outline", "Filled Outline"), desc = "Style of the box.")

    private var dojoType = DojoType.NONE
    private var targetSkeleton: net.minecraft.world.entity.Entity? = null  // Changed to Entity type
    private var lookCooldown = 0L
    private var lastSkeletonPos: Vec3? = null
    private var skeletonVel = Vec3.ZERO

    // Mastery state
    private val masteryBlocks = mutableListOf<MasteryBlock>()
    private var firingState = 0 // 0: Idle/Drawn, 1: Released (waiting to redraw)
    private var firingTimer = 0
    private var isDrawing = false

    private enum class DojoType {
        NONE, CONTROL, FORCE, MASTERY, DISCIPLINE
    }

    private data class MasteryBlock(
        val x: Int,
        val y: Int,
        val z: Int,
        val color: String,
        val expiryTime: Long
    )

    init {
    }

    override fun onEnable() {
        // Register event handlers
        registerEventHandlers()
    }

    // Event handlers - these need to be registered when the module is enabled
    private fun registerEventHandlers() {
        on<ChatPacketEvent> {
            if (!enabled) return@on

            // Try multiple detection patterns
            when {
                "Control" in value && ("OBJECTIVE" in value || "objective" in value.replace(Regex("§."), "").replace(Regex("\\s+"), "")) -> {
                    dojoType = DojoType.CONTROL
                    lastSkeletonPos = null
                }
                "Mastery" in value && ("OBJECTIVE" in value || "objective" in value.replace(Regex("§."), "").replace(Regex("\\s+"), "")) -> {
                    dojoType = DojoType.MASTERY
                    selectBow()
                }
                "Discipline" in value && ("OBJECTIVE" in value || "objective" in value.replace(Regex("§."), "").replace(Regex("\\s+"), "")) -> {
                    dojoType = DojoType.DISCIPLINE
                }
                "Rank:" in value || "rank:" in value.lowercase() -> {
                    dojoType = DojoType.NONE
                    targetSkeleton = null
                    masteryBlocks.clear()
                }
            }
        }

        on<TickEvent.End> {
            if (!enabled || dojoType == DojoType.NONE) return@on

            when (dojoType) {
                DojoType.CONTROL -> if (enableControl) handleControl()
                DojoType.MASTERY -> if (enableMastery) handleMastery()
                DojoType.DISCIPLINE -> if (enableDiscipline) handleDiscipline()
                else -> {}
            }
        }

        on<RenderEvent.Extract> {
            if (!enabled || dojoType == DojoType.NONE) return@on

            // Render control target
            if (dojoType == DojoType.CONTROL && targetSkeleton != null) {
                val entity = targetSkeleton!!
                val pos = entity.position()
                val aabb = AABB(
                    pos.x - 0.5, pos.y, pos.z - 0.5,
                    pos.x + 0.5, pos.y + 2.0, pos.z + 0.5
                )
                drawStyledBox(aabb, Colors.MINECRAFT_AQUA, renderStyle, false)
            }

            // Render mastery blocks
            if (dojoType == DojoType.MASTERY) {
                for (block in masteryBlocks) {
                    val aabb = AABB(
                        block.x.toDouble(), block.y.toDouble(), block.z.toDouble(),
                        block.x + 1.0, block.y + 1.0, block.z + 1.0
                    )
                drawStyledBox(aabb, Colors.MINECRAFT_RED, renderStyle, false)
                }
            }

            RotationUtils.update()
        }
    }

    override fun onDisable() {
        dojoType = DojoType.NONE
        targetSkeleton = null
        masteryBlocks.clear()
        RotationUtils.reset()
    }

    private fun handleControl() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        var closestSkeleton: net.minecraft.world.entity.Entity? = null  // Changed to Entity type
        var minDist = 25.0
        var skeletonsFound = 0

        // Find nearest wither skeleton (excluding decoys with redstone helmet)
        for (entity in level.entitiesForRendering()) {
            if (entity is WitherSkeleton || (entity is Skeleton && entity.type == EntityType.WITHER_SKELETON)) {
                skeletonsFound++
                val skeleton = entity

                // Check if it's a decoy (redstone helmet)
                val helmet = skeleton.getItemBySlot(EquipmentSlot.HEAD)
                val helmetName = helmet.item.toString().lowercase()
                if (helmetName.contains("redstone")) {
                    continue
                }

                val dist = player.position().distanceTo(skeleton.position())
                if (dist < minDist) {
                    minDist = dist
                    closestSkeleton = skeleton  // No casting needed - assign directly
                }
            }
        }


        targetSkeleton = closestSkeleton

        if (closestSkeleton != null) {
            // Calculate velocity for prediction
            val currentPos = closestSkeleton.position()
            if (lastSkeletonPos != null) {
                skeletonVel = currentPos.subtract(lastSkeletonPos!!)
            }
            lastSkeletonPos = currentPos

            val now = System.currentTimeMillis()
            if (now - lookCooldown > 40) {
                lookCooldown = now

                // Predict position based on slider preference
                val predX = currentPos.x + (skeletonVel.x * controlPredictionTicks)
                val predY = currentPos.y + (skeletonVel.y * 2) + 2.5
                val predZ = currentPos.z + (skeletonVel.z * controlPredictionTicks)

                setRotation(predX, predY, predZ)
            }
        }
    }

    private fun handleMastery() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val now = System.currentTimeMillis()

        // Clean up expired blocks and verify blocks still exist
        masteryBlocks.removeAll { block ->
            // Remove expired blocks
            if (block.expiryTime < now) {
                return@removeAll true
            }
            // Remove blocks that no longer exist (were already shot)
            val blockState = level.getBlockState(net.minecraft.core.BlockPos(block.x, block.y, block.z))
            if (blockState.block != Blocks.YELLOW_WOOL) {
                return@removeAll true
            }
            false
        }

        // Handle redrawing sequence (2 ticks delay)
        if (firingState == 1) {
            firingTimer++
            if (firingTimer >= 2) {
                // Hold right click to draw bow back
                mc.options.keyUse.setDown(true)
                isDrawing = true
                firingState = 0
                firingTimer = 0
            }
        }

        // Scan for yellow wool blocks
        scanForMasteryBlocks()

        if (masteryBlocks.isNotEmpty()) {
            val closest = masteryBlocks[0]

            // Pre-rotate to target (aim 1.1 blocks higher to compensate for drop)
            setRotation(closest.x + 0.5, closest.y + 1.1, closest.z + 0.5)

            // Ensure bow is held and drawn
            val bowSlot = findItemSlot("bow")
            if (bowSlot != -1) {
                // Switch to bow slot using accessor to avoid private field access
                (player.inventory as InventoryAccessor).setSelectedSlot(bowSlot)
                if (firingState == 0 && !isDrawing) {
                    // Hold right click to draw bow back
                    mc.options.keyUse.setDown(true)
                    isDrawing = true
                }
            }

            // Check if we should shoot (yellow blocks: shoot when < masteryShootDelay left)
            val timeRemaining = closest.expiryTime - now
            val shouldShoot = if (closest.color == "yellow") timeRemaining < masteryShootDelay else false

            if (shouldShoot && isDrawing) {
                // Release right click to shoot
                mc.options.keyUse.setDown(false)
                isDrawing = false
                firingState = 1
                firingTimer = 0
                masteryBlocks.removeAt(0)
            }
        }
    }

    private fun handleDiscipline() {
        val player = mc.player ?: return
        val level = mc.level ?: return

        var targetZombie: Zombie? = null
        var zombiesFound = 0

        // Find zombie in view cone (15 degree cone, 5 block range)
        for (entity in level.entitiesForRendering()) {
            if (entity is Zombie) {
                zombiesFound++
                val dist = player.position().distanceTo(entity.position())
                if (dist <= 5.0) {
                    val dx = entity.x - player.x
                    val dy = entity.y + 1.5 - (player.y + player.eyeHeight)
                    val dz = entity.z - player.z

                    val targetYaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
                    val targetPitch = Math.toDegrees(atan2(-dy, sqrt(dx * dx + dz * dz))).toFloat()

                    val yawDiff = abs(normalizeAngle(targetYaw - player.yRot))
                    val pitchDiff = abs(targetPitch - player.xRot)

                    if (yawDiff + pitchDiff < 15) {
                        targetZombie = entity
                        break
                    }
                }
            }
        }

        if (targetZombie != null) {
            // Check helmet to determine sword type
            val helmet = targetZombie.getItemBySlot(EquipmentSlot.HEAD)
            val helmetName = helmet.item.toString().lowercase()

            val targetSword = when {
                "leather" in helmetName -> "wooden_sword"
                "iron" in helmetName -> "iron_sword"
                "gold" in helmetName -> "golden_sword"
                "diamond" in helmetName -> "diamond_sword"
                else -> null
            }

            if (targetSword != null) {
                val swordSlot = findItemSlot(targetSword)
                if (swordSlot != -1) {
                    // Use accessor to avoid inventory move detection
                    (player.inventory as InventoryAccessor).setSelectedSlot(swordSlot)
                }
            }
        }
    }

    private fun scanForMasteryBlocks() {
        val player = mc.player ?: return
        val level = mc.level ?: return
        val now = System.currentTimeMillis()

        // Scan nearby blocks for yellow wool (within 25 block range)
        val playerPos = player.blockPosition()
        for (x in -25..25) {
            for (y in -10..10) {
                for (z in -25..25) {
                    val pos = playerPos.offset(x, y, z)
                    val dist = sqrt((x * x + z * z).toDouble())
                    if (dist > 25) continue

                    val blockState = level.getBlockState(pos)
                    if (blockState.block == Blocks.YELLOW_WOOL) {
                        // Check if already in queue
                        val isDuplicate = masteryBlocks.any {
                            it.x == pos.x && it.z == pos.z && it.color == "yellow"
                        }

                        if (!isDuplicate) {
                            masteryBlocks.add(
                                MasteryBlock(
                                    pos.x, pos.y, pos.z,
                                    "yellow",
                                    now + 3500 // 3.5s lifespan
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setRotation(x: Double, y: Double, z: Double) {
        val rot = RotationUtils.getRotation(x, y, z)
        RotationUtils.smartSmoothLook(rot.yaw, rot.pitch, 350)
    }

    private fun findItemSlot(name: String): Int {
        val player = mc.player ?: return -1
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (!stack.isEmpty) {
                val itemName = stack.item.toString().lowercase()
                if (name.lowercase() in itemName) {
                    return i
                }
            }
        }
        return -1
    }

    private fun selectBow() {
        val player = mc.player ?: return
        val bowSlot = findItemSlot("bow")
        if (bowSlot != -1) {
            // Use accessor to avoid private field access
            (player.inventory as InventoryAccessor).setSelectedSlot(bowSlot)
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360
        if (normalized > 180) normalized -= 360
        if (normalized < -180) normalized += 360
        return normalized
    }
}
