package it.wsh.cn.wshlibrary.http;


import java.util.HashMap;
import java.util.Map;

/**
 * author: wenshenghui
 * created on: 2018/8/3 12:13
 * description:
 * http请求的配置：请求头，超时配置，缓存配置
 * sDefaultConfig 为默认配置
 */
public class HttpConfig {

    private final static int CONNECT_TIME_OUT_DEFAULT = 3;
    private final static int READ_TIME_OUT_DEFAULT = 5;
    private final static int WRITE_TIME_OUT_DEFAULT = 5;
    private final static int NET_WORK_CACHE_TIMEOUT = 5;//有网缓存，默认5秒
    private final static int NO_NET_WORK_CACHE_TIMEOUT = 60 * 60 * 24 ; //离线缓存，默认一天

    private final static int NOT_DEFINED = -1;
    private int mUserConnectTimeout = NOT_DEFINED;
    private int mUserReadTimeout = NOT_DEFINED;
    private int mUserWriteTimeout = NOT_DEFINED;
    private int mNetWorkCacheTimeout = NET_WORK_CACHE_TIMEOUT;
    private int mNoNetWorkCacheTimeout = NO_NET_WORK_CACHE_TIMEOUT;

    private boolean mNeedNetWorkCache = true; //是否需要有网络时的缓存
    private boolean mNeedNoNetWorkCache = true; // 是否需要离线缓存

    private static HttpConfig sDefaultConfig = create(true);



    private Map<String, String> mHeaders = new HashMap<>();

    public static HttpConfig create(boolean needDefaultConfig) {
        HttpConfig httpConfig = new HttpConfig();
        if (needDefaultConfig) {
            httpConfig.initHeaderMap();
        }
        return httpConfig;
    }

    private HttpConfig(){}

    /**
     * 此处定义固定Header内容，与业务相关请走addHeader方法
     * 根据实际情况，也要做出调整
     */
    private void initHeaderMap() {
        mHeaders.put("Content-Type", "application/json");
        mHeaders.put("terminalType", "android");
        mHeaders.put("FrontType", "egc-mobile-ui");
        mHeaders.put("charset", "UTF-8");
        mHeaders.put("terminalVersion", "1.1.0");
        mHeaders.put("traceId", String.format("%s0200%s00000000000000000000000000000000",
                System.currentTimeMillis(), String.valueOf((int) ((Math.random()*9+1)*Math.pow(10, 6)))));
    }

    public HttpConfig addHeader(String name, String value) {
        mHeaders.put(name, value);
        return this;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    /**
     * 如果不需要init里面的数据，就clear
     */
    public void clear() {
        if (mHeaders != null) {
            mHeaders.clear();
        }
    }

    public boolean isNeedNetWorkCache() {
        return mNeedNetWorkCache;
    }

    /**
     * 设置不需要有网缓存
     */
    public void setNoNeedNetWorkCache() {
        this.mNeedNetWorkCache = false;
    }

    public boolean isNeedNoNetWorkCache() {
        return mNeedNoNetWorkCache;
    }

    /**
     * 设置不需要离线缓存
     */
    public void setNoNeedNoNetWorkCache() {
        this.mNeedNoNetWorkCache = false;
    }

    /**
     * 设置有网络时的缓存超时时间
     * @param netWorkCacheTimeout
     */
    public void setNetWorkCacheTimeout(int netWorkCacheTimeout) {
        this.mNetWorkCacheTimeout = netWorkCacheTimeout;
    }

    public int getNetWorkCacheTimeout() {
        if (mNetWorkCacheTimeout < 0){
            return NET_WORK_CACHE_TIMEOUT;
        }
        return mNetWorkCacheTimeout;
    }

    /**
     * 设置无网络时的缓存超时时间
     * @param noNetWorkCacheTimeout
     */
    public void setNoNetWorkCacheTimeout(int noNetWorkCacheTimeout) {
        this.mNoNetWorkCacheTimeout = noNetWorkCacheTimeout;
    }

    public int getNoNetWorkCacheTimeout() {
        if (mNoNetWorkCacheTimeout < 0){
            return NO_NET_WORK_CACHE_TIMEOUT;
        }
        return mNoNetWorkCacheTimeout;
    }

    public int getConnectTimeout() {
        if (mUserConnectTimeout <= 0){
            return CONNECT_TIME_OUT_DEFAULT;
        }
        return mUserConnectTimeout;
    }

    public HttpConfig connectTimeout(int connectTimeout) {
        this.mUserConnectTimeout = connectTimeout;
        return this;
    }

    public int getReadTimeout() {
        if (mUserReadTimeout <= 0){
            return READ_TIME_OUT_DEFAULT;
        }
        return mUserReadTimeout;
    }

    public HttpConfig readTimeout(int readTimeout) {
        this.mUserReadTimeout = readTimeout;
        return this;
    }

    public int getWriteTimeout() {
        if (mUserWriteTimeout <= 0){
            return WRITE_TIME_OUT_DEFAULT;
        }
        return mUserWriteTimeout;
    }

    public HttpConfig writeTimeout(int writeTimeout) {
        this.mUserWriteTimeout = writeTimeout;
        return this;
    }

    @Override
    public String toString() {
        return "HttpConfig{" +
                "mUserConnectTimeout='" + mUserConnectTimeout + '\'' +
                ", mUserReadTimeout='" + mUserReadTimeout + '\'' +
                ", mUserWriteTimeout='" + mUserWriteTimeout + '\'' +
                mHeaders.toString()+
                '}';
    }


    public static HttpConfig getDefault() {
        return sDefaultConfig;
    }
}
