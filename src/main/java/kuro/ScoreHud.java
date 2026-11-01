package kuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Ozellestirilebilir scoreboard HUD'u. Baslik, satir etiketleri/ikonlari ve renkler kuro_scoreboard.txt'den degistirilir;
 * DEGERLER yazilamaz, her satir bir "kaynak"tan oyundan gercek veri okur (fps, ping, x/y/z, sure, olum, totem, kristal, can...).
 *
 * Dosya bicimi:
 *   title=CHICKEN SMP
 *   titleColor=FFAA55
 *   satir: etiket|kaynak|etiketRengi|degerRengi        (renkler RRGGBB)
 */
public final class ScoreHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final String DEFAULT = """
      title=KURO
      titleColor=FFAA55
      FPS|fps|FFFFFF|55FF55
      PING|ping|FFFFFF|55FFFF
      KOORDİNAT|xyz|FFFFFF|FFFF55
      CAN|health|FFFFFF|FF5555
      TOTEM|totem|FFFFFF|FFAA00
      KRİSTAL|crystal|FFFFFF|AA55FF
      ÖLÜM|deaths|FFFFFF|FF5555
      OYNAMA|time|FFFFFF|FFFF55
      """;

   private static String title = "KURO";
   private static int titleColor = 0xFFAA55;
   private static final List<String[]> lines = new ArrayList<>();
   private static final long start = System.currentTimeMillis();
   private static int deaths;
   private static boolean wasDead;
   private static String[] cache;
   private static int cachedW;
   private static long lastUpd;
   private static boolean privacyCached;

   private ScoreHud() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getGameDir().resolve("kuro_scoreboard.txt");
   }

   public static void load() {
      Path f = file();
      try {
         if (!Files.exists(f)) {
            Files.writeString(f, DEFAULT);
         }

         lines.clear();
         cache = null;
         for (String raw : Files.readAllLines(f)) {
            String l = raw.trim();
            if (l.isEmpty() || l.startsWith("#")) {
               continue;
            }

            if (l.startsWith("title=")) {
               title = l.substring(6);
            } else if (l.startsWith("titleColor=")) {
               titleColor = parse(l.substring(11), 0xFFAA55);
            } else {
               String[] p = l.split("\\|");
               if (p.length >= 2) {
                  lines.add(new String[]{p[0], p[1].trim().toLowerCase(), p.length > 2 ? p[2] : "FFFFFF", p.length > 3 ? p[3] : "FFFFFF"});
               }
            }
         }
      } catch (IOException ignored) {
      }
   }

   private static int parse(String s, int def) {
      try {
         return Integer.parseInt(s.trim().replace("#", ""), 16) & 0xFFFFFF;
      } catch (Exception e) {
         return def;
      }
   }

   public static void render(DrawContext dc, RenderTickCounter tc) {
      if (!Kuro.on[Kuro.SCOREHUD] || mc.player == null || mc.world == null || mc.options.hudHidden) {
         return;
      }

      boolean dead = mc.player.getHealth() <= 0.0F;
      if (dead && !wasDead) {
         deaths++;
      }

      wasDead = dead;

      float s = Math.max(0.5F, Math.min(2.5F, Config.shScale));
      int pad = 6;
      int lh = 12;
      long now = System.nanoTime();
      if (cache == null || cache.length != lines.size() || now - lastUpd > 250_000_000L || privacyCached != (Config.shPrivacy >= 0.5F)) {
         lastUpd = now;
         privacyCached = Config.shPrivacy >= 0.5F;
         cache = new String[lines.size()];
         cachedW = mc.textRenderer.getWidth(title);
         for (int i = 0; i < lines.size(); i++) {
            cache[i] = privacyCached ? "•••" : value(lines.get(i)[1]);
            cachedW = Math.max(cachedW, mc.textRenderer.getWidth(lines.get(i)[0] + "  " + cache[i]));
         }
      }

      int w = cachedW;
      String[] vals = cache;
      int h = pad * 2 + lh * (lines.size() + 1) + 2;
      dc.getMatrices().push();
      dc.getMatrices().translate(Config.shX, Config.shY, 0.0F);
      dc.getMatrices().scale(s, s, 1.0F);
      dc.fill(0, 0, w + pad * 2, h, ((int)(Config.shAlpha / 100.0F * 255.0F) << 24) | 0x0A0A0A);
      int y = pad;
      dc.drawText(mc.textRenderer, title, (w + pad * 2 - mc.textRenderer.getWidth(title)) / 2, y, 0xFF000000 | titleColor, true);
      y += lh + 2;
      for (int i = 0; i < lines.size(); i++) {
         String[] l = lines.get(i);
         dc.drawText(mc.textRenderer, l[0], pad, y, 0xFF000000 | parse(l[2], 0xFFFFFF), true);
         dc.drawText(mc.textRenderer, vals[i], pad + w - mc.textRenderer.getWidth(vals[i]), y, 0xFF000000 | parse(l[3], 0xFFFFFF), true);
         y += lh;
      }

      dc.getMatrices().pop();
   }

   private static String value(String src) {
      switch (src) {
         case "fps":
            return String.valueOf(mc.getCurrentFps());
         case "ping": {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            return e == null ? "-" : e.getLatency() + "ms";
         }
         case "xyz":
            return mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
         case "health":
            return String.valueOf(Math.round(mc.player.getHealth() + mc.player.getAbsorptionAmount()));
         case "totem":
            return String.valueOf(count(Items.TOTEM_OF_UNDYING));
         case "crystal":
            return String.valueOf(count(Items.END_CRYSTAL));
         case "gapple":
            return String.valueOf(count(Items.GOLDEN_APPLE) + count(Items.ENCHANTED_GOLDEN_APPLE));
         case "pearl":
            return String.valueOf(count(Items.ENDER_PEARL));
         case "deaths":
            return String.valueOf(deaths);
         case "players":
            return String.valueOf(mc.world.getPlayers().size());
         case "time": {
            long sec = (System.currentTimeMillis() - start) / 1000L;
            return sec >= 3600L ? sec / 3600L + "s " + sec % 3600L / 60L + "d" : sec / 60L + "d " + sec % 60L + "sn";
         }
         default:
            return "?";
      }
   }

   private static int count(Item item) {
      PlayerInventory inv = mc.player.getInventory();
      int n = 0;
      for (int i = 0; i < inv.size(); i++) {
         ItemStack st = inv.getStack(i);
         if (st.isOf(item)) {
            n += st.getCount();
         }
      }

      return n;
   }
}
