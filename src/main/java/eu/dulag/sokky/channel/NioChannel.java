package eu.dulag.sokky.channel;

import eu.dulag.sokky.ByteBuf;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioChannel implements Channel {

    private final NioProvider provider;
    private final SocketChannel socket;

    private final SocketAddress localAddress, remoteAddress;

    private boolean connected;

    private ByteBuf alloc = ByteBuf.alloc(0);

    public NioChannel(NioProvider provider, SocketChannel socket) throws IOException {
        this.provider = provider;
        this.socket = socket;
        this.localAddress = socket.getLocalAddress();
        this.remoteAddress = socket.getRemoteAddress();
        this.connected = true;
    }

    @Override
    public void write(ByteBuf buf) {
        try {
            if (buf.readable() == 0) buf.flip();
            int readable = buf.readable();
            ByteBuffer buffer = ByteBuffer.allocate(readable + Integer.BYTES);
            buffer.putInt(readable);
            buffer.put(buf.context());

            socket.write((ByteBuffer) buffer.flip());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf read() {
        try {
            ByteBuffer buffer = provider.await();
            while (true) {
                try {
                    int read = socket.read(buffer);
                    if (read == -1) break;
                    if (read == 0) break;
                    alloc.enlarge(read).write((ByteBuffer) buffer.flip());
                    buffer.clear();
                    if (read < buffer.remaining()) break;
                } catch (Exception exception) {
                    break;
                }
            }
            provider.detach(buffer);

            alloc.flip();
            if (alloc.readable() == 0) return null;
            return alloc;
        } catch (InterruptedException exception) {
            return null;
        }
    }

    @Override
    public void close() {
        try {
            connected = false;
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeFuture() {
        alloc = null;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }
}