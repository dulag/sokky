package eu.dulag.sokky.channel;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NioProvider implements Closeable {

    private final ExecutorService service;
    private final boolean single;

    private final Selector selector;

    public NioProvider(int threads, Selector selector) {
        if (threads <= 1) threads = 1;
        this.service = Executors.newFixedThreadPool(threads);
        this.single = threads == 1;
        this.selector = selector;
    }

    public NioProvider(Selector selector) {
        this(8, selector);
    }

    public void select(Consumer<SelectionKey> consumer) {
        while (selector.isOpen()) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) service.submit(() -> consumer.accept(key));
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
        service.shutdown();
    }

    public boolean isMultithreading() {
        return single;
    }

    public boolean isOpen() {
        return selector.isOpen();
    }
}