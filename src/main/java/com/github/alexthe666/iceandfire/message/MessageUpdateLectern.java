package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityLectern;
import com.github.alexthe666.iceandfire.enums.EnumBestiaryPages;
import com.github.alexthe666.iceandfire.inventory.ContainerLectern;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

public class MessageUpdateLectern {

    public long blockPos;
    public int selectedPages1;
    public int selectedPages2;
    public int selectedPages3;
    public boolean updateStack;
    public int pageOrdinal;

    public MessageUpdateLectern(long blockPos, int selectedPages1, int selectedPages2, int selectedPages3, boolean updateStack, int pageOrdinal) {
        this.blockPos = blockPos;
        this.selectedPages1 = selectedPages1;
        this.selectedPages2 = selectedPages2;
        this.selectedPages3 = selectedPages3;
        this.updateStack = updateStack;
        this.pageOrdinal = pageOrdinal;

    }

    public MessageUpdateLectern() {
    }

    public static MessageUpdateLectern read(FriendlyByteBuf buf) {
        return new MessageUpdateLectern(buf.readLong(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readInt());
    }

    public static void write(MessageUpdateLectern message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        buf.writeInt(message.selectedPages1);
        buf.writeInt(message.selectedPages2);
        buf.writeInt(message.selectedPages3);
        buf.writeBoolean(message.updateStack);
        buf.writeInt(message.pageOrdinal);
    }

    private static final int PAGE_COST = 3;

    /**
     * A client picked one of the lectern's pages (ContainerLectern.clickMenuButton). The server checks everything here,
     * since a client can send this without anything else: the player has this lectern's menu open and is in range,
     * the page is one the lectern is offering (the server chose those), and the manuscripts are there. The manuscripts
     * are taken here too, together with the page (the button click used to take them, so sending only this packet
     * gave pages for free). Clients never choose the offered pages, so the selectedPages variant is ignored.
     */
    private static void addPageFromClient(Player player, TileEntityLectern lectern, MessageUpdateLectern message) {
        if (!message.updateStack || !(player.containerMenu instanceof ContainerLectern) || !lectern.stillValid(player)) {
            return;
        }
        EnumBestiaryPages page = EnumBestiaryPages.fromInt(message.pageOrdinal);
        if (page == null || !Arrays.asList(lectern.selectedPages).contains(page)) {
            return;
        }
        ItemStack bookStack = lectern.getItem(0);
        ItemStack manuscripts = lectern.getItem(1);
        if (bookStack.getItem() != IafItemRegistry.BESTIARY.get()) {
            return;
        }
        if (!player.isCreative()) {
            if (manuscripts.getItem() != IafItemRegistry.MANUSCRIPT.get() || manuscripts.getCount() < PAGE_COST) {
                return;
            }
            manuscripts.shrink(PAGE_COST);
            if (manuscripts.isEmpty()) {
                lectern.setItem(1, ItemStack.EMPTY);
            }
        }
        EnumBestiaryPages.addPage(page, bookStack);
        lectern.randomizePages(bookStack, lectern.getItem(1));
        lectern.setChanged();
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdateLectern message, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () -> MessageUpdateLectern.Handler.handlePacket(message, ctx))
            );
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MessageUpdateLectern.Handler.handlePacket(message, ctx))
            );
            ctx.get().setPacketHandled(true);
        }

        public static void handlePacket(final MessageUpdateLectern message, final Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                Player player = context.getSender();

                if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    player = IceAndFire.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().hasChunkAt(pos) && player.level().getBlockEntity(pos) instanceof TileEntityLectern lectern) {
                        if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
                            addPageFromClient(player, lectern, message);
                        } else if (message.updateStack) {
                            ItemStack bookStack = lectern.getItem(0);
                            if (bookStack.getItem() == IafItemRegistry.BESTIARY.get()) {
                                EnumBestiaryPages.addPage(EnumBestiaryPages.fromInt(message.pageOrdinal), bookStack);
                            }
                            lectern.randomizePages(bookStack, lectern.getItem(1));
                        } else {
                            lectern.selectedPages[0] = EnumBestiaryPages.fromInt(message.selectedPages1);
                            lectern.selectedPages[1] = EnumBestiaryPages.fromInt(message.selectedPages2);
                            lectern.selectedPages[2] = EnumBestiaryPages.fromInt(message.selectedPages3);
                        }
                    }
                }
            });

            context.setPacketHandled(true);
        }
    }

}