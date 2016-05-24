package com.daohoangson.pubvncast.networking;

import android.text.TextUtils;

import com.daohoangson.pubvncast.BuildConfig;

import java.io.UnsupportedEncodingException;

public class CorsProxy {

    public static boolean isConfigured() {
        return !TextUtils.isEmpty(BuildConfig.CORS_PROXY_URL);
    }

    public static String buildUrl(String url) throws UnsupportedEncodingException {
        if (!isConfigured()) {
            return url;
        }

        return String.format("%s/%s", BuildConfig.CORS_PROXY_URL, url.replaceAll("^http://", ""));
    }
}
