package com.daohoangson.pubvncast.networking;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

public class PubvnDecodeSrt {
    public static String buildUrl(String url) throws UnsupportedEncodingException {
        return String.format("https://pubvn-decode-srt.herokuapp.com/encoded/%s/sub.vtt",
                Base64.encodeToString(url.getBytes("UTF-8"), Base64.NO_WRAP));
    }
}
