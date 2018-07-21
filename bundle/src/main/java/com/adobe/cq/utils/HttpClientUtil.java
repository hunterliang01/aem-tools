package com.adobe.cq.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtil {

    private static final String UTF8 = "UTF-8";
    private static final String POST_ERROR = "POST request failed:";
    private static final String GET_ERROR = "GET request failed:";
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private void HttpClientUtil(){
        //empty constructor
    }

    /**
     * do Post without signature
     * @param url
     * @param jsonParam
     * @return
     */
    public static JSONObject doPost(String url, JSONObject jsonParam) {
        HttpClient httpClient = HttpClients.createDefault();
        JSONObject jsonResult = null;
        HttpPost httpPost = new HttpPost(url);
        try {
            if (null != jsonParam) {
                StringEntity entity = new StringEntity(jsonParam.toString(), UTF8);
                entity.setContentEncoding(UTF8);
                entity.setContentType("application/json");
                httpPost.setEntity(entity);
            }
            HttpResponse result = httpClient.execute(httpPost);
            url = URLDecoder.decode(url, UTF8);
            if (result.getStatusLine().getStatusCode() == 200) {
                String str = "";
                try {
                    str = EntityUtils.toString(result.getEntity());
                    jsonResult = new JSONObject(str);
                } catch (Exception e) {
                    logger.error(POST_ERROR + url, e);
                }
            }
        } catch (IOException e) {
            logger.error(POST_ERROR + url, e);
        }
        return jsonResult;
    }

    /**
     * 不需要使用签名，不需要参数
     * @param url
     * @return JSONObject
     */
    public static JSONObject doGet(String url) {
        JSONObject jsonResult = null;
        try {
            HttpClient httpClient = HttpClients.createDefault();
//            String strResult = "";
            //发送get请求
            HttpGet request = new HttpGet(url);
            jsonResult = executeRequest(httpClient,request);

        } catch (IOException e) {
            logger.error(GET_ERROR + url, e);
        } catch (JSONException e) {
            logger.error(POST_ERROR + url, e);
        } catch (ParseException e) {
            logger.error(POST_ERROR + url, e);
        }
        return jsonResult;
    }
    
    /**
     * 不需要使用签名，不需要参数
     * @param url
     * @return HTML
     */
    public static String doGetWithHtml(HttpClient httpClient,HttpClientContext localContext,String url) {
    	
        try {
        	HttpGet request = new HttpGet(url);// 创建get请求
            HttpResponse response;
            response = httpClient.execute(request,localContext);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity, UTF8);
                }
            } else {
                logger.error(GET_ERROR + url);
                return null;
            }
        } catch (IOException e) {
            logger.error(GET_ERROR + url, e);
        }
        return null;
    }

    /**
     * 发送get请求，参数为JSONObject对象
     * @param url 请求的URL
     * @param jObj parameter
     * @return JSONObject
     */
    public static JSONObject doGet(String url, JSONObject jObj) {
        url = setAbsoluteUrl(url, jObj);
        return doGet(url);
    }


    /**
     * set absolute url
     * @param url
     * @param jObj parameters
     * @return absolute url with parameter
     */
    private static String setAbsoluteUrl(String url, JSONObject jObj) {
        StringBuilder sb = new StringBuilder("");
        if (jObj != null) {
            try {
                Iterator<?> ite = jObj.keys();
                while (ite.hasNext()) {
                    String key = (String) ite.next();
                    sb.append(key + "=" + jObj.get(key) + "&");
                }
                sb.substring(0, sb.length() - 1);
            } catch (JSONException e) {
                logger.error("Error when setting absolute url", e);
            }
        }
        if (sb.length() > 0) {
            url += "?" + sb.toString();
        }
        return url;
    }

    private static JSONObject executeRequest(HttpClient httpClient,HttpGet request) throws IOException, ParseException, JSONException {
        JSONObject jsonResult = null;
        String strResult = "";
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                strResult = EntityUtils.toString(entity, getContentCharSet(entity));
            }
            jsonResult = new JSONObject(strResult);
        } else {
            logger.error(GET_ERROR + request.getURI());
        }
        return jsonResult;
    }

    private static String getContentCharSet(final HttpEntity entity)
            throws ParseException {

        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        String charset = null;
        if (entity.getContentType() != null) {
            HeaderElement values[] = entity.getContentType().getElements();
            if (values.length > 0) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    charset = param.getValue();
                }
            }
        }

        if (StringUtils.isEmpty(charset)) {
            charset = UTF8;
        }
        return charset;
    }

    public static void autenticate(HttpClient httpclient,HttpClientContext localContext,String domain,String authUrl,String username,String password){
    	CookieStore cookieStore = new BasicCookieStore();
//    	localContext = new HttpClientContext();
//    	httpclient = HttpClients.createDefault();
    	localContext.setCookieStore(cookieStore);
    	HttpPost httppost = new HttpPost(domain+authUrl);

        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair("j_username", username));
        formParams.add(new BasicNameValuePair("j_password", password));
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
            httppost.setEntity(entity);
            // 提交登录数据
            HttpResponse re = httpclient.execute(httppost, localContext);
            EntityUtils.toString(re.getEntity());
            // 获取cookie
            List<Cookie> cook = cookieStore.getCookies();
            for(Cookie c:cook){
                //设置cookie
                BasicClientCookie cookie = new BasicClientCookie(c.getName(), c.getValue());   
                  cookie.setVersion(0);    
                  cookie.setDomain(domain);   //设置范围  
                  cookie.setPath("/");   
                  cookieStore.addCookie(cookie); 
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
