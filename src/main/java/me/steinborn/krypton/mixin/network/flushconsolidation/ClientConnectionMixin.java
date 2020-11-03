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
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Optimizes ClientConnection by adding the ability to skip auto-flushing and using void promises where possible.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ConfigurableAutoFlush {
    @Shadow private Channel channel;

    @Shadow @Final public static AttributeKey<NetworkState> ATTR_KEY_PROTOCOL;
    @Shadow private int packetsSentCounter;
    @Shadow @Final private static Logger LOGGER;
    private boolean shouldAutoFlush = true;

    @Shadow public abstract void setState(NetworkState state);

    /**
     * Refactored sendImmediately method.
     * @author Andrew Steinborn
     */
    @Overwrite
    private void sendImmediately(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        NetworkState networkState = NetworkState.getPacketHandlerState(packet);
        NetworkState networkState2 = (NetworkState) this.channel.attr(ATTR_KEY_PROTOCOL).get();
        ++this.packetsSentCounter;
        boolean newState = networkState2 != networkState;
        if (newState) {
            LOGGER.debug("Disabled auto read");
            this.channel.config().setAutoRead(false);
        }

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

    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(ClientConnection clientConnection) {
        return null;
    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, boolean shouldFlush) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.writeAndFlush(packet);
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
