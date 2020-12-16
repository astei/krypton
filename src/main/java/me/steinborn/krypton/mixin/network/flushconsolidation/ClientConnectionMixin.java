package me.steinborn.krypton.mixin.network.flushconsolidation;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import me.steinborn.krypton.mod.network.ConfigurableAutoFlush;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optimizes ClientConnection by adding the ability to skip auto-flushing and using void promises where possible.
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements ConfigurableAutoFlush {
    @Shadow private Channel channel;
    private AtomicBoolean shouldAutoFlush;

    @Shadow public abstract void setState(NetworkState state);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddedFields(CallbackInfo ci) {
        this.shouldAutoFlush = new AtomicBoolean(true);
    }

    /**
     * Refactored sendImmediately method. This is a better fit for {@code @Overwrite} but we have to write it this way
     * because the fabric-networking-api-v1 also mixes into this...
     *
     * @author Andrew Steinborn
     */
    @Inject(locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true,
            method = "sendImmediately",
            at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;packetsSentCounter:I", opcode = Opcodes.GETFIELD))
    private void sendImmediately$rewrite(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo info, NetworkState packetState, NetworkState protocolState) {
        boolean newState = packetState != protocolState;

        if (this.channel.eventLoop().inEventLoop()) {
            if (newState) {
                this.setState(packetState);
            }
            doSendPacket(packet, callback);
        } else {
            if (newState) {
                this.channel.config().setAutoRead(false);
            }
            this.channel.eventLoop().execute(() -> {
                if (newState) {
                    this.setState(packetState);
                }
                doSendPacket(packet, callback);
            });
        }

        info.cancel();
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    public Channel disableForcedFlushEveryTick(ClientConnection clientConnection) {
        return null;
    }

    private void doSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback) {
        if (callback == null) {
            this.channel.write(packet, this.channel.voidPromise());
        } else {
            ChannelFuture channelFuture = this.channel.write(packet);
            channelFuture.addListener(callback);
            channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }

        if (this.shouldAutoFlush.get()) {
            this.channel.flush();
        }
    }

    @Override
    public void setShouldAutoFlush(boolean shouldAutoFlush) {
        boolean wasAutoFlushing = this.shouldAutoFlush.getAndSet(shouldAutoFlush);
        if (!wasAutoFlushing) {
            this.channel.flush();
        }
    }
}
