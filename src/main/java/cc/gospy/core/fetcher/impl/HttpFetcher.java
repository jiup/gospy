/*
 * Copyright 2017 ZhangJiupeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.gospy.core.fetcher.impl;

import cc.gospy.core.Page;
import cc.gospy.core.Task;
import cc.gospy.core.fetcher.Fetcher;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpFetcher implements Fetcher {

    public static final int TIMEOUT = 3000;
    public static final HttpFetcher DefaultHttpFetcher = new HttpFetcher();

    public static class Builder {
        private HttpFetcher fetcher;

        private Builder() {
            fetcher = new HttpFetcher();
        }

        public Builder beforeRequest(BeforeRequest request) {
            fetcher.configurator = request;
            return this;
        }

        public Builder setMaxConnCount(int maxConnCount) {
            fetcher.maxConnCount = maxConnCount;
            return this;
        }

        public Builder setMaxConnPerRoute(int maxConnPerRoute) {
            fetcher.maxConnPerRoute = maxConnPerRoute;
            return this;
        }

        public Builder setCleanPeriodSeconds(int cleanPeriodSeconds) {
            fetcher.cleanPeriodSeconds = cleanPeriodSeconds;
            return this;
        }

        public Builder setConnExpireSeconds(int connExpireSeconds) {
            fetcher.connExpireSeconds = connExpireSeconds;
            return this;
        }

        public Builder setAutoKeepAlive(boolean autoKeepAlive) {
            fetcher.autoKeepAlive = autoKeepAlive;
            return this;
        }

        public Builder setUseProxy(boolean useProxy) {
            fetcher.useProxy = useProxy;
            return this;
        }

        public Builder setProxyAddress(InetSocketAddress proxyAddress) {
            fetcher.proxyAddress = proxyAddress;
            return this;
        }

        public HttpFetcher build() {
            try {
                fetcher.init();
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return fetcher;
        }
    }

    public static Builder custom() {
        return new Builder();
    }

    private int maxConnCount = 200;
    private int maxConnPerRoute = 10;
    private int cleanPeriodSeconds = 300;
    private int connExpireSeconds = 30;
    private boolean autoKeepAlive = true;
    private boolean useProxy = false;
    private InetSocketAddress proxyAddress = new InetSocketAddress("localhost", 1080);

    private PoolingHttpClientConnectionManager connectionManager;
    private PoolingHttpClientConnectionCleaner cleanerThread;

    protected class PoolingHttpClientConnectionCleaner extends Thread {
        private final HttpClientConnectionManager connectionManager;
        private volatile boolean running;
        private int expireSeconds;

        private PoolingHttpClientConnectionCleaner(HttpClientConnectionManager manager, int expireSeconds) {
            this.connectionManager = manager;
            this.expireSeconds = expireSeconds;
            this.running = true;
        }

        @Override
        public void run() {
            while (running) {
                synchronized (this) {
                    try {
                        wait(cleanPeriodSeconds * 1000);
                        connectionManager.closeExpiredConnections();
                        connectionManager.closeIdleConnections(expireSeconds, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void shutdown() {
            running = false;
            synchronized (this) {
                notifyAll();
            }
        }

    }

    protected void init() throws KeyManagementException, NoSuchAlgorithmException {
        if (useProxy) {
            connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder
                    .<ConnectionSocketFactory>create()
                    .register("http", new ProxyPlainConnectionSocketFactory())
                    .register("https", new ProxySSLConnectionSocketFactory(getWeakenedSSLContextInstance()))
                    .build()
            );
        } else {
            connectionManager = new PoolingHttpClientConnectionManager(RegistryBuilder
                    .<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", new SSLConnectionSocketFactory(getWeakenedSSLContextInstance()))
                    .build()
            );
        }
        if (autoKeepAlive) {
            connectionManager.setMaxTotal(maxConnCount);
            connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);
            client = getHttpClientInstance();
            cleanerThread = new PoolingHttpClientConnectionCleaner(connectionManager, connExpireSeconds);
            cleanerThread.start();
        }
    }

    protected class ProxyPlainConnectionSocketFactory implements ConnectionSocketFactory {

        @Override
        public Socket createSocket(HttpContext httpContext) throws IOException {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
            return new Socket(proxy);
        }

        @Override
        public Socket connectSocket(
                final int connectTimeout,
                final Socket socket, final HttpHost host,
                final InetSocketAddress remoteAddress,
                final InetSocketAddress localAddress, final HttpContext context
        ) throws IOException {
            Socket socket0 = socket != null ? socket : createSocket(context);
            if (localAddress != null) {
                socket0.bind(localAddress);
            }
            try {
                socket0.connect(remoteAddress, connectTimeout);
            } catch (SocketTimeoutException e) {
                throw new ConnectTimeoutException(e, host, remoteAddress.getAddress());
            }
            return socket0;
        }
    }

    protected class ProxySSLConnectionSocketFactory extends SSLConnectionSocketFactory {

        public ProxySSLConnectionSocketFactory(SSLContext sslContext) {
            super(sslContext);
        }

        @Override
        public Socket createSocket(HttpContext httpContext) throws IOException {
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddress);
            return new Socket(proxy);
        }
    }

    @FunctionalInterface
    public interface BeforeRequest {
        void setRequestConfig(HttpRequestBase request);
    }

    private BeforeRequest configurator;
    private CloseableHttpClient client;

    private HttpFetcher() {
        this(request -> request.setConfig(RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setConnectionRequestTimeout(TIMEOUT)
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT).build())
        );
    }

    private HttpFetcher(BeforeRequest configurator) {
        this.configurator = configurator;
    }

    private CloseableHttpClient getHttpClientInstance() {
        HttpRequestRetryHandler requestRetryHandler = (e, i, httpContext) -> i < 3 && e instanceof NoHttpResponseException;
        return HttpClients.custom().setConnectionManager(connectionManager).setRetryHandler(requestRetryHandler).build();
    }

    private SSLContext getWeakenedSSLContextInstance() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("SSLv3");
        context.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }}, null);
        return context;
    }

    protected CloseableHttpResponse doGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        configurator.setRequestConfig(request);
        return client.execute(request);
    }

    protected CloseableHttpResponse doPost(String url, Map<String, Object> attributes) throws IOException {
        HttpPost request = new HttpPost(url);
        configurator.setRequestConfig(request);
        List<NameValuePair> pairs = new ArrayList<>();
        attributes.keySet().forEach(key -> {
            pairs.add(new BasicNameValuePair(key, attributes.get(key).toString()));
        });
        request.setEntity(new UrlEncodedFormEntity(pairs));
        return client.execute(request);
    }

    private void exit() {
        if (autoKeepAlive) {
            cleanerThread.shutdown();
        }
    }

    @Override
    public Page fetch(Task task) throws Throwable {
        Page page = new Page();
        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

        long start = System.currentTimeMillis();
        if (!autoKeepAlive) {
            this.init();
            client = getHttpClientInstance();
        }
        CloseableHttpResponse response;
        Map<String, Object> extra = task.getExtra();
        response = extra != null ? doPost(task.getUrl(), extra) : doGet(task.getUrl());
        page.setResponseTime(System.currentTimeMillis() - start);
        page.setStatusCode(response.getStatusLine().getStatusCode());
        HttpEntity entity = response.getEntity();
        page.setMimeType(entity.getContentType().getValue());
        entity.writeTo(responseBody);
        page.setContent(responseBody);
        extra = new HashMap<>();
        for (Header header : response.getAllHeaders()) {
            extra.put(header.getName(), header.getValue());
        }
        page.setExtra(extra);

        response.close();
        if (!autoKeepAlive) {
            client.close();
        }

        task.addVisitCount();
        task.setLastVisitTime(System.currentTimeMillis());
        return page;
    }
}
