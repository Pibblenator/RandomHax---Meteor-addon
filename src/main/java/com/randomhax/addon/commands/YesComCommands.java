package com.randomhax.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.randomhax.addon.modules.YesCom;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class YesComCommands {
    private YesComCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal(".lastpos")
                    .then(argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            FabricClientCommandSource src = ctx.getSource();
                            String query = StringArgumentType.getString(ctx, "name");
                            var lp = YesCom.LAST_SEEN.get(query.toLowerCase(Locale.ROOT));
                            if (lp == null) {
                                src.sendFeedback(Text.of("[YesCom] No lastpos stored for " + query + "."));
                                return 1;
                            }

                            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lp.timeMs));
                            long ageMs = System.currentTimeMillis() - lp.timeMs;
                            String age = humanAge(ageMs);

                            src.sendFeedback(Text.of(String.format(
                                "[YesCom] %s last seen at %d %d %d (%s) — %s ago (%s).",
                                lp.name, lp.pos.getX(), lp.pos.getY(), lp.pos.getZ(), lp.dim, age, ts
                            )));
                            return 1;
                        })
                    )
            );
        });
    }

    private static String humanAge(long ms) {
        long d = TimeUnit.MILLISECONDS.toDays(ms);
        long h = TimeUnit.MILLISECONDS.toHours(ms) % 24;
        long m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }
}
