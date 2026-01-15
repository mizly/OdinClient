package starred.skies.odin.features.impl.cheats

import com.odtheking.odin.clickgui.settings.impl.*
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.containsOneOf
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ClickType
import starred.skies.odin.OdinClient
import starred.skies.odin.utils.Skit

object AutoSell : Module(
    name = "Auto Sell",
    description = "Automatically sell items in trades and cookie menus. (/autosell)",
    category = Skit.CHEATS
) {
    val sellList by ListSetting("Sell list", mutableSetOf<String>())
    private val delay by NumberSetting("Delay", 6, 2, 10, 1, desc = "The delay between each sell action.", unit = " ticks")
    private val randomization by NumberSetting("Randomization", 1, 0, 5, 1, desc = "Random delay variance", unit = " ticks")
    private val clickType1 by SelectorSetting("Click Type", "Shift", arrayListOf("Shift", "Middle", "Left"), desc = "The type of click to use when selling items.")
    private val addDefaults by ActionSetting("Add defaults", desc = "Add default dungeon items to the auto sell list.") {
        sellList.addAll(defaultItems)
        modMessage("§aAdded default items to auto sell list")
        OdinClient.moduleConfig.save()
    }

    private var lastClickTime = 0L

    init {
        schedule()
    }

    private fun schedule() {
        val randomDelay = delay + (if (randomization > 0) (0..randomization).random() else 0)

        schedule(randomDelay) {
            click()
            schedule()
        }
    }

    private fun click() {
        if (!enabled || sellList.isEmpty()) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 50) return

        val container = mc.screen as? AbstractContainerScreen<*> ?: return
        val player = mc.player ?: return
        val title = container.title?.string ?: return

        if (!title.equalsOneOf("Trades", "Booster Cookie", "Farm Merchant", "Ophelia")) return

        val slots = container.menu.slots
        if (slots.size < 90) return

        val index = slots
            .subList(54, 90.coerceAtMost(slots.size))
            .firstOrNull { slot ->
                val stack = slot.item
                if (stack.isEmpty) return@firstOrNull false
                stack.hoverName.string.noControlCodes.containsOneOf(sellList, ignoreCase = true)
            }?.index ?: return

        val clickType = when (clickType1) {
            0 -> ClickType.QUICK_MOVE
            1 -> ClickType.CLONE
            2 -> ClickType.PICKUP
            else -> ClickType.QUICK_MOVE
        }

        mc.gameMode?.handleInventoryMouseClick(container.menu.containerId, index, 0, clickType, player)
        lastClickTime = currentTime
    }

    private val defaultItems = arrayOf(
        "enchanted ice", "superboom tnt", "rotten", "skeleton master", "skeleton grunt", "cutlass",
        "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
        "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
        "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
        "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
        "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
        "healing viii splash potion", "healing 8 splash potion", "candycomb"
    )
}