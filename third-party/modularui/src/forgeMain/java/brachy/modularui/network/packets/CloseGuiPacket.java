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
public class CloseGuiPacket implements NetworkHandler.INetPacket {

    private int networkId;
    private boolean dispose;

    public CloseGuiPacket(FriendlyByteBuf buffer) {
        this.networkId = buffer.readVarInt();
        this.dispose = buffer.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.networkId);
        buffer.writeBoolean(this.dispose);
    }

    @Override
    public void execute(NetworkEvent.Context handler) {
        if (handler.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ModularNetwork.CLIENT.closeContainer(this.networkId, this.dispose, MCHelper.getPlayer(), false);
        } else {
            ModularNetwork.SERVER.closeContainer(this.networkId, this.dispose, handler.getSender(), false);
        }
    }
}
