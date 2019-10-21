package com.ifeng.mcn.spider.webmagic;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.downloader.HttpClientRequestContext;
import us.codecraft.webmagic.downloader.HttpUriRequestConverter;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.ProxyProvider;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.CharsetUtils;
import us.codecraft.webmagic.utils.HttpClientUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 *  返回  cookie 下载器
 *
 * @author chenghao1
 * @create 2019/8/30
 * @since 1.0.0
 * Copyright © 2012 Phoenix New Media Limited All Rights Reserved.
 */
public class CookieHttpClientDownloader  extends AbstractDownloader {


    private CookieStore cookieStore = new BasicCookieStore();

    public CookieStore getCookieStore() {
        return cookieStore;
    }
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private SpiderHttpClientGenerator httpClientGenerator = new SpiderHttpClientGenerator();
    private HttpUriRequestConverter httpUriRequestConverter = new HttpUriRequestConverter();
    private ProxyProvider proxyProvider;
    private boolean responseHeader = true;
    public CookieHttpClientDownloader() {

    }

    public void setProxyProvider(ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    private CloseableHttpClient getHttpClient(Site site, CookieStore  cookieStore) {
        return httpClientGenerator.generateClient(site,cookieStore) ;
    }

    @Override
    public Page download(Request request, Task task  ) {
        if (task != null && task.getSite() != null) {
            CloseableHttpResponse httpResponse = null;
            CloseableHttpClient httpClient = this.getHttpClient(task.getSite(),cookieStore);
            Proxy proxy = this.proxyProvider != null ? this.proxyProvider.getProxy(task) : null;
            HttpClientRequestContext requestContext = this.httpUriRequestConverter.convert(request, task.getSite(), proxy);
            Page page = Page.fail();

            Page var9;
            try {
                httpResponse = httpClient.execute(requestContext.getHttpUriRequest(), requestContext.getHttpClientContext());
                page = this.handleResponse(request, request.getCharset() != null ? request.getCharset() : task.getSite().getCharset(), httpResponse, task);
                this.onSuccess(request);
                this.logger.info("downloading page success {}", request.getUrl());
                Page var8 = page;
                return var8;
            } catch (IOException e) {
                this.logger.warn("download page {} error", request.getUrl(), e);
                this.onError(request);
                page.setRawText(e.getMessage()) ;
                var9 = page;

            } finally {
                if (httpResponse != null) {
                    EntityUtils.consumeQuietly(httpResponse.getEntity());
                }

                if (this.proxyProvider != null && proxy != null) {
                    this.proxyProvider.returnProxy(proxy, page, task);
                }

            }

            return var9;
        } else {
            throw new NullPointerException("task or site can not be null");
        }
    }

    @Override
    public void setThread(int thread) {
        this.httpClientGenerator.setPoolSize(thread);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
        byte[] bytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
        String contentType = httpResponse.getEntity().getContentType() == null ? "" : httpResponse.getEntity().getContentType().getValue();
        Page page = new Page();
        page.setBytes(bytes);
        if (!request.isBinaryContent()) {
            if (charset == null) {
                charset = this.getHtmlCharset(contentType, bytes);
            }

            page.setCharset(charset);
            page.setRawText(new String(bytes, charset));
        }

        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        page.setDownloadSuccess(true);
        if (this.responseHeader) {
            page.setHeaders(HttpClientUtils.convertHeaders(httpResponse.getAllHeaders()));
        }

        return page;
    }

    private String getHtmlCharset(String contentType, byte[] contentBytes) throws IOException {
        String charset = CharsetUtils.detectCharset(contentType, contentBytes);
        if (charset == null) {
            charset = Charset.defaultCharset().name();
            this.logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()", Charset.defaultCharset());
        }

        return charset;
    }
}
