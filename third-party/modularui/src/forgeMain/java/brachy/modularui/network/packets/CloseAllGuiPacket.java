package brachy.modularui.network.packets;

import brachy.modularui.api.MCHelper;
import brachy.modularui.network.ModularNetwork;
import brachy.modularui.network.NetworkHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CloseAllGuiPacket implements NetworkHandler.INetPacket {

    public CloseAllGuiPacket(FriendlyByteBuf buffer) {}

    @Override
    public void encode(FriendlyByteBuf buffer) {}

    @Override
    public void execute(NetworkEvent.Context handler) {
        if (handler.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ModularNetwork.CLIENT.closeAll(MCHelper.getPlayer(), false);
        } else {
            ModularNetwork.SERVER.closeAll(handler.getSender(), false);
        }
    }
}
