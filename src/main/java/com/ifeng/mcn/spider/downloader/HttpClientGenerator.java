package com.ifeng.mcn.spider.downloader;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.CustomRedirectStrategy;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

/**
 *  支持 https
 *
 * @author chenghao1
 * @create 2019/1/29
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class HttpClientGenerator {

    private transient Logger logger = LoggerFactory.getLogger(this.getClass());
    public PoolingHttpClientConnectionManager connectionManager;

    public HttpClientGenerator() {
        RegistryBuilder registryBuilder = (RegistryBuilder)RegistryBuilder.create() ;
        Registry<ConnectionSocketFactory> reg = registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE).register("https", this.buildSSLConnectionSocketFactory()).build();
        this.connectionManager = new PoolingHttpClientConnectionManager(reg);
        this.connectionManager.setDefaultMaxPerRoute(20);
    }

    private SSLConnectionSocketFactory buildSSLConnectionSocketFactory() {
        try {
            return new SSLConnectionSocketFactory(createIgnoreVerifySSL(), new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"},
                    null,
                    new DefaultHostnameVerifier());
        } catch (KeyManagementException e) {
            logger.error("ssl connection fail", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("ssl connection fail", e);
        }
        return SSLConnectionSocketFactory.getSocketFactory();
    }

    private SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        SSLContext sc = SSLContext.getInstance("SSLv3");
        sc.init((KeyManager[])null, new TrustManager[]{trustManager}, (SecureRandom)null);
        return sc;
    }

    public HttpClientGenerator setPoolSize(int poolSize) {
        this.connectionManager.setMaxTotal(poolSize);
        return this;
    }

    public CloseableHttpClient getClient(Site site) {
        return this.generateClient(site);
    }

    private CloseableHttpClient generateClient(Site site) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setConnectionManager(this.connectionManager);
        if (site.getUserAgent() != null) {
            httpClientBuilder.setUserAgent(site.getUserAgent());
        } else {
            httpClientBuilder.setUserAgent("");
        }

        if (site.isUseGzip()) {
            httpClientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {
                public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                    if (!request.containsHeader("Accept-Encoding")) {
                        request.addHeader("Accept-Encoding", "gzip");
                    }

                }
            });
        }

        httpClientBuilder.setRedirectStrategy(new CustomRedirectStrategy());
        SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
        socketConfigBuilder.setSoKeepAlive(true).setTcpNoDelay(true);
        socketConfigBuilder.setSoTimeout(site.getTimeOut());
        SocketConfig socketConfig = socketConfigBuilder.build();
        httpClientBuilder.setDefaultSocketConfig(socketConfig);
        this.connectionManager.setDefaultSocketConfig(socketConfig);
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(site.getRetryTimes(), true));
        this.generateCookie(httpClientBuilder, site);
        return httpClientBuilder.build();
    }

    private void generateCookie(HttpClientBuilder httpClientBuilder, Site site) {
        if (site.isDisableCookieManagement()) {
            httpClientBuilder.disableCookieManagement();
        } else {
            CookieStore cookieStore = new BasicCookieStore();
            Iterator var4 = site.getCookies().entrySet().iterator();

            Map.Entry domainEntry;
            while(var4.hasNext()) {
                domainEntry = (Map.Entry)var4.next();
                BasicClientCookie cookie = new BasicClientCookie((String)domainEntry.getKey(), (String)domainEntry.getValue());
                cookie.setDomain(site.getDomain());
                cookieStore.addCookie(cookie);
            }

            var4 = site.getAllCookies().entrySet().iterator();

            while(var4.hasNext()) {
                domainEntry = (Map.Entry)var4.next();
                Iterator var9 = ((Map)domainEntry.getValue()).entrySet().iterator();

                while(var9.hasNext()) {
                    Map.Entry<String, String> cookieEntry = (Map.Entry)var9.next();
                    BasicClientCookie cookie = new BasicClientCookie((String)cookieEntry.getKey(), (String)cookieEntry.getValue());
                    cookie.setDomain((String)domainEntry.getKey());
                    cookieStore.addCookie(cookie);
                }
            }

            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }
    }
}
