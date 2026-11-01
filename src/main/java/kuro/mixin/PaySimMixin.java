package kuro.mixin;

import kuro.PaySim;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zorunlu olmayan mixin dosyasinda (kuro.extra.mixins.json): imza uymazsa oyun cokmez, sadece simulasyon calismaz. */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class PaySimMixin {
   @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true, require = 0)
   private void kuro$payChat(String command, CallbackInfo ci) {
      if (PaySim.intercept(command)) {
         ci.cancel();
      }
   }

   @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true, require = 0)
   private void kuro$payCmd(String command, CallbackInfoReturnable<Boolean> cir) {
      if (PaySim.intercept(command)) {
         cir.setReturnValue(true);
      }
   }
}
