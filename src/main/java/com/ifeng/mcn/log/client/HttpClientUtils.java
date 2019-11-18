/**
 *@Copyright ISM
 *@author WANGXiang 
 *2015-03-02
 *
 */
package com.ifeng.mcn.log.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.*;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client utilities,Simple use for download or upload TEXT with default
 * HTTP settings, also support to remove the idle connection.
 */
public class HttpClientUtils {
    public static final String CONTENT_TYPE_APP_JSON = "application/json";
    public final static int CONNECT_TIMEOUT = 5000;
    public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
    public static final int SOCKET_TIMEOUT = 5000;
    public static final String CONTENT_TYPE_JSON = "text/json";
    public static final String CONTENT_TYPE_OCT = "application/oct-stream";

    public static final int DEFAULT_MAX_TOTAOL_CONS = 1000;
    public static final int DEFAULT_PER_ROUTE = 200;

    protected PoolingHttpClientConnectionManager connManager = null;
    protected CloseableHttpClient httpclient = null;
    IdleConnectionMonitorThread idleConnectionMonitorThread = null;

    private static HttpClientUtils me;

    static {
        getInstance();
    }

    protected HttpClientUtils() {

    }

    /**
     * GLOBAL SHARED,will start a thread for close idle connection
     * 
     * @return a closeable http client
     */
    public static HttpClientUtils getInstance() {
        if (me == null || me.httpclient == null) {
            me = new HttpClientUtils();
            try {
                SSLContext sslContext = SSLContexts.custom().build();
                sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } }, null);
                Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                        .<ConnectionSocketFactory> create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", SSLConnectionSocketFactory.getSystemSocketFactory()).build();
                //暂时不使用keep alive策略，看具体情况
                ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy() {
                    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                        // Honor 'keep-alive' header
                        // HeaderElementIterator it = new
                        // BasicHeaderElementIterator(
                        // response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                        // while (it.hasNext()) {
                        // HeaderElement he = it.nextElement();
                        // String param = he.getName();
                        // String value = he.getValue();
                        // if (value != null &&
                        // param.equalsIgnoreCase("timeout")) {
                        // try {
                        // return Long.parseLong(value) * 1000;
                        // } catch(NumberFormatException ignore) {
                        // }
                        // }
                        // }
                        // HttpHost target = (HttpHost)
                        // context.getAttribute(HttpClientContext.HTTP_TARGET_HOST);
                        // if ("".equalsIgnoreCase(target.getHostName())) {
                        // // Keep alive for 5 seconds only
                        // return 5 * 1000;
                        // } else {
                        // // otherwise keep alive for 30 seconds
                        // return 30 * 1000;
                        // }
                        return 5 * 1000;
                    }
                };
                me.connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                // Create socket configuration
                SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).build();
                me.connManager.setDefaultSocketConfig(socketConfig);
                // Create message constraints
                MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200)
                        .setMaxLineLength(2000).build();
                // Create connection configuration
                ConnectionConfig connectionConfig = ConnectionConfig.custom()
                        .setMalformedInputAction(CodingErrorAction.IGNORE)
                        .setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(StandardCharsets.UTF_8)
                        .setMessageConstraints(messageConstraints).build();
                me.connManager.setDefaultConnectionConfig(connectionConfig);
                me.connManager.setMaxTotal(DEFAULT_MAX_TOTAOL_CONS);
                me.connManager.setDefaultMaxPerRoute(DEFAULT_PER_ROUTE);
//                me.connManager.setValidateAfterInactivity(3000);// 检查halfclose
                HttpClientBuilder builder = HttpClients.custom();
                builder.setRetryHandler(new HttpRequestRetryHandler() {
                    @Override
                    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                        if (executionCount > 3) {
                            System.err.println("Maximum tries reached for client http pool ");
                            return false;
                        }
                        if (exception instanceof org.apache.http.NoHttpResponseException) {
                            System.err.println("No response from server on " + executionCount + " call");
                            return true;
                        }
                        return false;
                    }
                });
                me.httpclient = builder.setConnectionManager(me.connManager).build();
                me.startIldeConnectionManagerThread();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        }
        return me;
    }

    public byte[] post(String url, byte[] buf, int off, int len) throws IOException {
        return post(url, CONNECT_TIMEOUT, buf, off, len);
    }

    public byte[] post(String url, int timeout, byte[] buf, int off, int len) throws IOException {

        HttpPost post = new HttpPost(url);
        try {
            post.setHeader("Content-type", CONTENT_TYPE_OCT);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout).setExpectContinueEnabled(false).build();
            post.setConfig(requestConfig);

            post.setEntity(new ByteArrayEntity(buf, off, len));
            CloseableHttpResponse response = httpclient.execute(post);
            try {
                HttpEntity entity = response.getEntity();
                try {
                    if (entity != null) {
                        byte[] str = EntityUtils.toByteArray(entity);
                        return str;
                    }
                } finally {
                    if (entity != null) {
                        entity.getContent().close();
                    }
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }finally {
            post.releaseConnection();
        }
        return new byte[0];
    }

    /**
     * POST and get the response content,decompress if need.(the HTTP response
     * header)
     * 
     * @param url
     *            HTTP URL
     * @param content
     *            json,need to post
     * @param isGzip
     *            is need add {@code HttpHeaders.ACCEPT_ENCODING} to HTTP Header
     * @return if header returned by Sever set the header Content-Encoding to
     *         GZIP, then return the decompress content,otherwise the plain text
     *         of HTTP response
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public String postJSON(String url, String content, boolean isGzip, Map<String, String> addHeaders) throws SocketTimeoutException, IOException {
        return post(url, CONNECT_TIMEOUT, content, CONTENT_TYPE_APP_JSON, DEFAULT_ENCODING, isGzip, addHeaders);
    }

    /**
     * 
     * @param url
     * @param content
     * @param contentType
     * @param httpCode
     *            ignored. useless.
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public String post(String url, String content, String contentType, Integer httpCode, Map<String, String> addHeaders)
            throws SocketTimeoutException, IOException {
        return post(url, CONNECT_TIMEOUT, content, contentType, DEFAULT_ENCODING, false, addHeaders);
    }

    /**
     * 
     * @param url
     * @param timeout
     * @param content
     * @param contentType
     * @param httpCode
     *            ignored. useless.
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public String post(String url, int timeout, String content, String contentType, Integer httpCode, Map<String, String> addHeaders)
            throws SocketTimeoutException, IOException {
        return post(url, timeout, content, contentType, DEFAULT_ENCODING, false, addHeaders);
    }

    /**
     * 
     * @param url
     * @param timeout socket超时和连接超时
     * @param content body
     * @param contentType http content-type
     * @param charset  字符集
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public String post(String url, int timeout, String content, String contentType, Charset charset, boolean isOutputGZip,boolean isInputGZip, Map<String, String> addHeaders)
            throws SocketTimeoutException, IOException {

        HttpPost post = new HttpPost(url);
        try {
            post.setHeader("Content-type", contentType + ";charset=" + charset.name());
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout).setExpectContinueEnabled(false).build();
            post.setConfig(requestConfig);
            if (isOutputGZip) {
                post.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            }
            
            
            if(isInputGZip){
                post.addHeader(HttpHeaders.CONTENT_ENCODING,"GZIP");
                post.setEntity(new GzipCompressingEntity(new StringEntity(content, charset)));
            }
            else{
                post.setEntity(new StringEntity(content, charset));
            }
            if (null != addHeaders && addHeaders.size() > 0) {
                addHeaders.forEach((k, v) -> post.addHeader(k, v));
            }
            CloseableHttpResponse response = httpclient.execute(post);
            try {
                Header header = response.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
                HttpEntity entity = response.getEntity();
                // httpCode = response.getStatusLine().getStatusCode();
                try {
                    if (entity != null) {
                        String str = EntityUtils.toString(entity, charset);
                        return str;
                    }
                } finally {
                    if (entity != null) {
                        entity.getContent().close();
                    }
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } finally {
            post.releaseConnection();
        }
        return "";
    }
    /**
     * 
     * @param url
     * @param timeout socket超时和连接超时
     * @param content body
     * @param contentType http content-type
     * @param charset  字符集
     * @return
     * @throws SocketTimeoutException
     * @throws IOException
     */
    public String post(String url, int timeout, String content, String contentType, Charset charset, boolean isGZip, Map<String, String> addHeaders)
            throws SocketTimeoutException, IOException {

       return post(url,timeout,content,contentType,charset,false,false, addHeaders);
    }

    public String get(String url) throws ClientProtocolException, IOException {
        return get(url, null, CONNECT_TIMEOUT, SOCKET_TIMEOUT, DEFAULT_ENCODING);
    }

    public String get(String url, int connectTimeout) throws ClientProtocolException, IOException {
        return get(url, null, connectTimeout, SOCKET_TIMEOUT, DEFAULT_ENCODING);
    }

    public String get(String url, Map<String, Object> params) throws ClientProtocolException, IOException {
        return get(url, params, CONNECT_TIMEOUT, SOCKET_TIMEOUT, DEFAULT_ENCODING);
    }

    public String get(String url, Map<String, Object> params, int connectTimeout)
            throws ClientProtocolException, IOException {
        return get(url, params, connectTimeout, SOCKET_TIMEOUT, DEFAULT_ENCODING);
    }

    public String get(String url, Map<String, Object> params, int connectTimeout, int soTimeout, Charset charset,
            boolean isGzip) throws ClientProtocolException, IOException {
        String responseString = null;
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(connectTimeout)
                .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).build();

        StringBuilder sb = new StringBuilder();
        sb.append(url);
        int i = 0;
        if (params != null) {
            for (Entry<String, Object> entry : params.entrySet()) {
                if (i == 0 && !url.contains("?")) {
                    sb.append("?");
                } else {
                    sb.append("&");
                }
                sb.append(entry.getKey());
                sb.append("=");
                String value = entry.getValue() == null ? "" : entry.getValue().toString();
                try {
                    sb.append(URLEncoder.encode(value, charset.name()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }
        HttpGet get = new HttpGet(sb.toString());
        get.setConfig(requestConfig);
        if (isGzip) {
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
        }else
        {
            get.addHeader(HttpHeaders.ACCEPT_ENCODING, "identity");
        }
        CloseableHttpResponse response = httpclient.execute(get);
        try {
            Header header = response.getFirstHeader(HttpHeaders.CONTENT_ENCODING);
            HttpEntity entity = response.getEntity();
            // httpCode = response.getStatusLine().getStatusCode();
            try {
                if (entity != null) {
                    String str = EntityUtils.toString(entity, charset);
                    return str;
                }
            } finally {
                if (entity != null) {
                    entity.getContent().close();
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return responseString;
    }

    public String get(String url, Map<String, Object> params, int connectTimeout, int soTimeout, Charset encode)
            throws ClientProtocolException, IOException {
        return get(url,params,connectTimeout,soTimeout,encode,false);
    }

    public String get(String url, Map<String, Object> params, int connectTimeout, int soTimeout, String encoding)
            throws ClientProtocolException, IOException {
        return get(url,params,connectTimeout,soTimeout,Charset.forName(encoding),false);
    }
    /**
     * 自动关闭无效连接，避免TIME_WAIT2状态
     * 
     * @author Free
     *
     */
    public final static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(final HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
            this.setDaemon(true);
            shutdown = false;

        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(15000);
                        // 关闭失效的连接
                        connMgr.closeExpiredConnections();
                        // 可选的, 关闭30秒内不活动的连接
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public PoolingHttpClientConnectionManager connectionManager() {
        return connManager;
    }

    public synchronized void startIldeConnectionManagerThread() {
        if (me.idleConnectionMonitorThread == null) {
            me.idleConnectionMonitorThread = new IdleConnectionMonitorThread(me.connManager);
        }
        me.idleConnectionMonitorThread.start();
    }

    public synchronized void shutdownIldeConnectionManagerThread() {
        if (me.idleConnectionMonitorThread != null && !me.idleConnectionMonitorThread.shutdown) {
            me.idleConnectionMonitorThread.shutdown();
        }
    }
}