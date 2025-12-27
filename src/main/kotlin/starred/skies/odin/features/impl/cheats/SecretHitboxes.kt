package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.SkullBlockEntity
import starred.skies.odin.utils.Skit
import java.util.UUID

object SecretHitboxes : Module(
    name = "Secret Hitboxes",
    description = "Extends the hitboxes of secret blocks to a full block.",
    category = Skit.CHEATS
) {
    val lever by BooleanSetting("Lever", false, desc = "Extends the lever hitbox.")
    val button by BooleanSetting("Button", false, desc = "Extends the button hitbox.")
    val essence by BooleanSetting("Essence", false, desc = "Extends the essence hitbox.")
    val chests by BooleanSetting("Chests", false, desc = "Extends the chest hitbox.")

    private val mostSignificantBits = UUID.fromString("e0f3e929-869e-3dca-9504-54c666ee6f23").mostSignificantBits

    @JvmStatic
    fun isEssence(pos: BlockPos): Boolean {
        if (!enabled) return false
        if (!essence) return false
        //? if >= 1.21.10 {
        /*return (mc.level?.getBlockEntity(pos) as? SkullBlockEntity)?.ownerProfile?.partialProfile()?.id?.mostSignificantBits == mostSignificantBits
        *///? } else {
        return (mc.level?.getBlockEntity(pos) as? SkullBlockEntity)?.ownerProfile?.gameProfile()?.id?.mostSignificantBits == mostSignificantBits
        //? }
    }
}