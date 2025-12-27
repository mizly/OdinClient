package starred.skies.odin.utils

import com.odtheking.mixin.accessors.KeyMappingAccessor
import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.KeyMapping

fun rightClick() {
    val key = mc.options.keyUse
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}

fun leftClick() {
    val key = mc.options.keyAttack
    val actualKey = (key as KeyMappingAccessor).boundKey
    KeyMapping.set(actualKey, true)
    KeyMapping.click(actualKey)
    KeyMapping.set(actualKey, false)
}