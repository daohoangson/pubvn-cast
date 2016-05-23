package com.daohoangson.pubvncast.networking;

import java.io.UnsupportedEncodingException;

public class CorsProxy {
    public static String buildUrl(String url) throws UnsupportedEncodingException {
        return String.format("http://125.212.247.216:1337/%s",
                url.replaceAll("^http://", ""));
    }
}
