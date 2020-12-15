package me.steinborn.krypton.mixin.network.flushconsolidation;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.steinborn.krypton.mod.network.ConfigurableAutoFlush;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes ClientConnection by adding the ability to skip auto-flushing and using void promises where possible.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ConfigurableAutoFlush {
    @Shadow private Channel channel;

    @Shadow @Final public static AttributeKey<NetworkState> ATTR_KEY_PROTOCOL;
    private boolean shouldAutoFlush = true;

    @Shadow public abstract void setState(NetworkState state);

    /**
     * Refactored sendImmediately method. THis is a better fit for {@code @Overwrite} but we have to write it this way
     * because the fabric-networking-api-v1 also mixes into this...
     *
     * @author Andrew Steinborn
     */
    @Inject(cancellable = true, method = "sendImmediately", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;packetsSentCounter:I"))
    private void sendImmediately$rewrite(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo info) {
        NetworkState networkState = NetworkState.getPacketHandlerState(packet);
        NetworkState networkState2 = (NetworkState) this.channel.attr(ATTR_KEY_PROTOCOL).get();
        boolean newState = networkState2 != networkState;

        if (this.channel.eventLoop().inEventLoop()) {
            if (networkState != networkState2) {
                this.setState(networkState);
            }
            doSendPacket(packet, callback, newState || this.shouldAutoFlush);
        } else {
            this.channel.eventLoop().execute(() -> {
                if (networkState != networkState2) {
                    this.setState(networkState);
                }
                doSendPacket(packet, callback, newState || this.shouldAutoFlush);
            });
        }

        info.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(ClientConnection clientConnection) {
        return null;
    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, boolean shouldFlush) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.write(packet);
            channelFuture.addListener(callback);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        if (shouldFlush) {
            this.channel.flush();
        }
    }

    @Override
    public void setShouldAutoFlush(boolean shouldAutoFlush) {
        boolean wasAutoFlushing = this.shouldAutoFlush;
        this.shouldAutoFlush = shouldAutoFlush;
        if (!wasAutoFlushing) {
            this.channel.flush();
        }
    }
}
