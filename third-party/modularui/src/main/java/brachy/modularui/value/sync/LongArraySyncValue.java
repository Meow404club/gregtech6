package brachy.modularui.value.sync;

import brachy.modularui.utils.serialization.network.ByteBufAdapters;

//? if neoforge {
import io.netty.buffer.ByteBuf;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

//? if neoforge {
public class LongArraySyncValue extends GenericSyncValue<ByteBuf, long[]> {
//?} else {
/*public class LongArraySyncValue extends GenericSyncValue<long[]> {
*///?}

    public LongArraySyncValue(@NotNull Supplier<long[]> getter, @Nullable Consumer<long[]> setter) {
        this(getter, setter, false);
    }

    public LongArraySyncValue(@NotNull Supplier<long[]> getter, @Nullable Consumer<long[]> setter, boolean nullable) {
        super(long[].class, getter, setter, ByteBufAdapters.LONG_ARR, long[]::clone, nullable);
    }
}
