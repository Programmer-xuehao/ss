package com.ifeng.mcn.spider.utils;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author linlvping
 * @date 2016-04-27
 */
public class HttpUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);
    private final static int SOCK_TIMEOUT = 40;
    private final static int CONN_TIMEOUT = 40;
    private final static RequestConfig config = RequestConfig.custom().setSocketTimeout(SOCK_TIMEOUT * 1000)
            .setConnectTimeout(CONN_TIMEOUT * 1000).build();
    private static final int HTTP_SUCCESS = 200;
    private static final String CHARSET_UTF8 = "UTF-8";

    public static String get(String url) {
        HttpGet method = new HttpGet(url) ;
        return executeMethod(method);
    }
    public static String put(String url) {
        HttpPut method = new HttpPut(url) ;
        return executeMethod(method);
    }

    public static String get(String url, Map<String, ?> paramsMap) {
        if (paramsMap != null) {
            Set<String> keySet = paramsMap.keySet();
            StringBuilder sb = new StringBuilder(keySet.size() * 8);
            sb.append("?");
            for (String key : keySet) {
                try {
                    String value = URLEncoder.encode(String.valueOf(paramsMap.get(key)), CHARSET_UTF8);
                    sb.append(key).append("=").append(value).append("&");
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            String params = sb.toString();
            url += params.substring(0, params.length() - 1);
        }
        HttpGet method = new HttpGet(url);
        return executeMethod(method);
    }

    public static String post(String url, Map<String, ?> paramsMap) {
        HttpPost method = new HttpPost(url);
        if (paramsMap != null) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>(paramsMap.size());
            Set<String> keySet = paramsMap.keySet();
            for (String key : keySet) {
                nvps.add(new BasicNameValuePair(key, paramsMap.get(key).toString()));
            }
            try {
                method.setEntity(new UrlEncodedFormEntity(nvps, CHARSET_UTF8));
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return executeMethod(method);
    }

    public static String postXml(String url, String body) {
        HttpPost method = new HttpPost(url);
        method.setEntity(new StringEntity(body, ContentType.create("text/xml", CHARSET_UTF8)));
        return executeMethod(method);
    }

    public static String postJson(String url, String body) {
        HttpPost method = new HttpPost(url);
        //System.out.println(url);
        //System.out.println(body);
        method.setEntity(new StringEntity(body, ContentType.create("application/json", CHARSET_UTF8)));
        return executeMethod(method);
    }

    private static final String executeMethod(HttpRequestBase method) {
        CloseableHttpClient client = HttpClients.createDefault();
        method.setConfig(config);
        try {
            HttpResponse response = client.execute(method);
            if (HTTP_SUCCESS == response.getStatusLine().getStatusCode()) {
                return EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
            } else {
                String error = EntityUtils.toString(response.getEntity(), CHARSET_UTF8);
                logger.warn(error);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            method.releaseConnection();
            try {
                client.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }


    public static String getRequestIp(HttpServletRequest request) {
        String ipAddress = null;
        ipAddress = request.getHeader("x-forwarded-for");
        if (!isValidIp(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (!isValidIp(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!isValidIp(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        if (ipAddress != null && ipAddress.length() > 15) {
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

    private static boolean isValidIp(String ip) {
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return true;
    }
}
