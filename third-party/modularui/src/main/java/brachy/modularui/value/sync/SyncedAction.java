package brachy.modularui.value.sync;

import brachy.modularui.api.ISyncedAction;

//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
//?} else {
/*import net.minecraft.network.FriendlyByteBuf;
*///?}

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class SyncedAction {

    private final ISyncedAction action;
    @Getter
    private final boolean executeClient;
    @Getter
    private final boolean executeServer;

    public SyncedAction(ISyncedAction action, boolean executeClient, boolean executeServer) {
        this.action = action;
        this.executeClient = executeClient;
        this.executeServer = executeServer;
    }

    //? if neoforge {
    public boolean invoke(boolean client, @NotNull RegistryFriendlyByteBuf packet) {
    //?} else {
    /*    public boolean invoke(boolean client, @NotNull FriendlyByteBuf packet) {
    *///?}
        if (isExecute(client)) {
            this.action.invoke(packet);
            return true;
        }
        return false;
    }

    public boolean isExecute(boolean client) {
        return (client && this.executeClient) || (!client && this.executeServer);
    }
}
