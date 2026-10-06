package com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

/**
 * Fetches bounded resources with checked DNS, explicit redirects and an overall deadline.
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
public class SafeHttpFetcher {
    private final PublicUrlPolicy policy;
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "preview-deadlines");
        thread.setDaemon(true);
        return thread;
    });

    public RemoteDocument fetch(String url, int maxBytes) {
        URI uri = policy.parse(url);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        // A dedicated client has no caller credentials, cookie store or system proxy.
        try (var client = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(new DnsResolver() {
                            @Override public InetAddress[] resolve(String host) throws UnknownHostException {
                                return policy.resolve(host);
                            }
                            @Override public String resolveCanonicalHostname(String host) { return host; }
                        })
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .setSocketTimeout(Timeout.ofSeconds(5)).build()).build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.ofSeconds(5))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5)).build())
                .disableRedirectHandling().disableAutomaticRetries().disableCookieManagement()
                .disableContentCompression().build()) {
            for (int hop = 0; hop <= 3; hop++) {
                // Literal addresses may bypass a client's resolver, so check them here too.
                policy.resolve(uri.getHost());
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) throw LinkPreviewException.fetchFailed();
                HttpGet request = new HttpGet(uri);
                request.setHeader("User-Agent", "SurveyPlatform-LinkPreview/1.0");
                request.setHeader("Accept", "text/html,application/xhtml+xml,image/*");
                request.setHeader("Accept-Encoding", "identity");
                var cancellation = DEADLINES.schedule(request::cancel, remaining, TimeUnit.NANOSECONDS);
                URI current = uri;
                try {
                    var result = client.execute(request, response -> {
                        int status = response.getCode();
                        if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                            var location = response.getFirstHeader("Location");
                            if (location == null) throw LinkPreviewException.fetchFailed();
                            // Cancel rather than draining an untrusted redirect body.
                            request.cancel();
                            return new RemoteDocument(policy.parse(current.resolve(location.getValue()).toString()), null, null);
                        }
                        if (status != 200 || response.getEntity() == null) {
                            request.cancel();
                            throw LinkPreviewException.fetchFailed();
                        }
                        var entity = response.getEntity();
                        if (entity.getContentLength() > maxBytes) {
                            request.cancel();
                            throw LinkPreviewException.fetchFailed();
                        }
                        try (var input = entity.getContent()) {
                            byte[] bytes = input.readNBytes(maxBytes + 1);
                            if (bytes.length > maxBytes) {
                                request.cancel();
                                throw LinkPreviewException.fetchFailed();
                            }
                            return new RemoteDocument(current, entity.getContentType(), bytes);
                        }
                    });
                    if (result.bytes() != null) return result;
                    uri = result.uri();
                } finally {
                    cancellation.cancel(false);
                }
            }
            throw LinkPreviewException.fetchFailed();
        } catch (LinkPreviewException exception) {
            throw exception;
        } catch (IOException | IllegalArgumentException exception) {
            throw LinkPreviewException.fetchFailed();
        }
    }
}
