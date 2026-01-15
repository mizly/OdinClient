package starred.skies.odin.commands

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import starred.skies.odin.OdinClient
import starred.skies.odin.features.impl.cheats.AutoSell.sellList

val autoSellCommand = Commodore("autosell") {
    literal("add").runs { item: GreedyString? ->
        val lowercase = item?.string?.lowercase() ?: mc.player?.mainHandItem?.displayName?.string?.noControlCodes?.lowercase() ?: return@runs modMessage("Either hold an item or write an item name to be added to autosell.")
        if (lowercase in sellList) return@runs modMessage("$lowercase is already in the Auto sell list.")

        modMessage("Added $lowercase to the Auto sell list.")
        sellList.add(lowercase)
        OdinClient.moduleConfig.save()
    }

    literal("remove").runs { item: GreedyString ->
        val lowercase = item.string.lowercase()
        if (lowercase !in sellList) return@runs modMessage("$item isn't in the Auto sell list.")

        modMessage("Removed $item from the Auto sell list.")
        sellList.remove(lowercase)
        OdinClient.moduleConfig.save()
    }

    literal("clear").runs {
        modMessage("Auto sell list cleared.")
        sellList.clear()
        OdinClient.moduleConfig.save()
    }

    literal("list").runs {
        if (sellList.isEmpty()) return@runs modMessage("Auto sell list is empty")
        val chunkedList = sellList.chunked(10)
        modMessage("Auto sell list:\n${chunkedList.joinToString("\n")}")
    }
}