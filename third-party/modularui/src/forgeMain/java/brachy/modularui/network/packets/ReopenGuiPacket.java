package brachy.modularui.network.packets;

import brachy.modularui.api.MCHelper;
import brachy.modularui.network.ModularNetwork;
import brachy.modularui.network.NetworkHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ReopenGuiPacket implements NetworkHandler.INetPacket {

    private int networkId;

    public ReopenGuiPacket(FriendlyByteBuf buffer) {
        this.networkId = buffer.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(networkId);
    }

    @Override
    public void execute(NetworkEvent.Context handler) {
        if (handler.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ModularNetwork.CLIENT.reopen(MCHelper.getPlayer(), this.networkId, false);
        } else {
            ModularNetwork.SERVER.reopen(handler.getSender(), this.networkId, false);
        }
    }
}
