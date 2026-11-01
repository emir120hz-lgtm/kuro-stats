package kuro;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Odeme simulasyonu (UI/test icin): "/pay <oyuncu> <miktar>" sunucuya GONDERILMEZ, para gitmez.
 * Sohbette yalnizca sende gorunen, basinda sabit "[TEST]" etiketi ve sonunda "(simülasyon)" yazan bir mesaj cikar.
 * Mesaj rengi menuden ayarlanir (R/G/B). Etiket ve simulasyon notu kapatilamaz.
 */
public final class PaySim {
   private PaySim() {
   }

   /** true donerse komut iptal edilir (sunucuya gitmez). cmd basinda "/" olmadan gelir. */
   public static boolean intercept(String cmd) {
      if (!Kuro.on[Kuro.PAYSIM] || cmd == null) {
         return false;
      }

      String[] p = cmd.trim().split("\\s+");
      if (p.length != 3 || !p[0].equalsIgnoreCase("pay")) {
         return false;
      }

      String amount = p[2];
      if (!amount.matches("[0-9]{1,12}([.,][0-9]{1,2})?[kKmMbB]?")) {
         return false;
      }

      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player == null) {
         return false;
      }

      int rgb = (clamp(Config.psR) << 16) | (clamp(Config.psG) << 8) | clamp(Config.psB);
      MutableText msg = Text.literal("[TEST] ").setStyle(Style.EMPTY.withColor(0xAAAAAA).withBold(true));
      msg.append(Text.literal(p[1] + " oyuncusuna " + amount + "$ gönderildi (simülasyon)").setStyle(Style.EMPTY.withColor(rgb)));
      mc.inGameHud.getChatHud().addMessage(msg);
      return true;
   }

   private static int clamp(float v) {
      return Math.max(0, Math.min(255, Math.round(v)));
   }
}
