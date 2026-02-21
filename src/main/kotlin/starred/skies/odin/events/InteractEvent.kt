package starred.skies.odin.events

import com.odtheking.odin.events.core.CancellableEvent
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack

sealed class InteractEvent {
    data class HitBlock(
        val item: ItemStack,
        val pos: BlockPos
    ) : CancellableEvent()
}