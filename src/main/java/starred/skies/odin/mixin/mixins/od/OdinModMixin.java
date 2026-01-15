package starred.skies.odin.mixin.mixins.od;

import com.odtheking.odin.OdinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OdinMod.class, remap = false)
public class OdinModMixin {
    @Inject(method = "onInitializeClient", at = @At(value = "INVOKE", target = "Lkotlinx/coroutines/BuildersKt;launch$default(Lkotlinx/coroutines/CoroutineScope;Lkotlin/coroutines/CoroutineContext;Lkotlinx/coroutines/CoroutineStart;Lkotlin/jvm/functions/Function2;ILjava/lang/Object;)Lkotlinx/coroutines/Job;"), remap = false, cancellable = true)
    private void removeTelemetry(CallbackInfo ci) {
        ci.cancel();
    }
}