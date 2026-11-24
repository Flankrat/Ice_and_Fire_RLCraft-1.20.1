package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.block.DragonForgeType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: the server's Dragonforge Mode. Sent while a player logs in and on /reload, before recipes and
 * tags (OnDatapackSyncEvent), so the bestiary's forge pages, the creative tab, JEI and the client's view of whether a
 * forge is assembled follow the server and not the player's own config file. The client drops it when it disconnects.
 */
public class MessageSyncDragonforgeMode {

    public final boolean individual;

    public MessageSyncDragonforgeMode(boolean individual) {
        this.individual = individual;
    }

    public static MessageSyncDragonforgeMode read(FriendlyByteBuf buf) {
        return new MessageSyncDragonforgeMode(buf.readBoolean());
    }

    public static void write(MessageSyncDragonforgeMode message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.individual);
    }

    public static void handle(MessageSyncDragonforgeMode message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DragonForgeType.setServerMode(message.individual ? "individual" : "global");
            refreshCreativeTabs();
        });
        context.setPacketHandled(true);
    }

    /** The creative tabs cache their contents until their parameters change; clear that so they are rebuilt for the new mode. */
    public static void refreshCreativeTabs() {
        try {
            ObfuscationReflectionHelper.setPrivateValue(CreativeModeTabs.class, null, null, "f_268496_");
        } catch (RuntimeException ignored) {
            // the tabs are rebuilt with the new mode next time their parameters change anyway
        }
    }
}
