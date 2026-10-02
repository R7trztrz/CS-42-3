package com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import org.springframework.stereotype.Component;
import java.net.*;
import java.util.concurrent.*;

/**
 * Rejects non-public destinations before sockets connect, including redirect targets.
 *
 * @author Simon Tian
 */
@Component
public class PublicUrlPolicy {
    // Bound resolver work even when the operating system's DNS lookup stalls.
    private static final ExecutorService DNS = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), task -> {
                Thread thread = new Thread(task, "preview-dns");
                thread.setDaemon(true);
                return thread;
            });
    public URI parse(String value) {
        try {
            if (value == null || value.length() > 2048) throw LinkPreviewException.invalidUrl();
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            boolean https = "https".equalsIgnoreCase(scheme);
            if ((!https && !"http".equalsIgnoreCase(scheme)) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getHost().contains("%")
                    || (uri.getPort() != -1 && uri.getPort() != (https ? 443 : 80))) {
                throw LinkPreviewException.invalidUrl();
            }
            // Fragments never belong in an HTTP request.
            return new URI(uri.toASCIIString().split("#", 2)[0]);
        } catch (URISyntaxException exception) {
            throw LinkPreviewException.invalidUrl();
        }
    }

    public InetAddress[] resolve(String host) throws UnknownHostException {
        Future<InetAddress[]> lookup;
        try { lookup = DNS.submit(() -> InetAddress.getAllByName(host)); }
        catch (RejectedExecutionException exception) { throw LinkPreviewException.fetchFailed(); }
        InetAddress[] addresses;
        try {
            addresses = lookup.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw LinkPreviewException.fetchFailed();
        } catch (ExecutionException | TimeoutException exception) {
            throw LinkPreviewException.fetchFailed();
        } finally {
            lookup.cancel(true);
        }
        for (InetAddress address : addresses) {
            if (!isPublic(address)) throw LinkPreviewException.invalidUrl();
        }
        return addresses;
    }

    public boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] b = address.getAddress();
        int a = b[0] & 255, c = b[1] & 255;
        if (b.length == 4) {
            // Deny private, shared, documentation, benchmarking and reserved IPv4 ranges.
            return a != 0 && a != 10 && a != 127 && a < 224
                    && !(a == 100 && c >= 64 && c <= 127)
                    && !(a == 169 && c == 254) && !(a == 172 && c >= 16 && c <= 31)
                    && !(a == 192 && (c == 168 || c == 0 || c == 2 || c == 88 && (b[2] & 255) == 99))
                    && !(a == 198 && (c == 18 || c == 19 || c == 51 && (b[2] & 255) == 100))
                    && !(a == 203 && c == 0 && (b[2] & 255) == 113);
        }
        // Allow global IPv6 unicast only; exclude transition and special-purpose allocations.
        return (a & 0xe0) == 0x20
                && !(a == 0x20 && c == 0x01 && (b[2] & 255) < 2)
                && !(a == 0x20 && c == 0x01 && (b[2] & 255) == 0x0d && (b[3] & 255) == 0xb8)
                && !(a == 0x20 && c == 0x02)
                && !(a == 0x3f && c == 0xff);
    }
}
