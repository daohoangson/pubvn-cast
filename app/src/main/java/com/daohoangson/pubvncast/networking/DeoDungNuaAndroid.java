package com.daohoangson.pubvncast.networking;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class DeoDungNuaAndroid {

    public static final String root = "http://deodungnua.bup.vn/api";

    public static void login(final Context context, String username, String passwordMd5, final DeoDungNuaV2.FilmListener<String> listener) throws UnsupportedEncodingException {
        String url = String.format("%s/login/&username=%s&password=%s",
                root,
                URLEncoder.encode(username, "utf-8"),
                URLEncoder.encode(passwordMd5, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String accessToken = null;

                if (response.has("accessToken")) {
                    try {
                        accessToken = response.getString("accessToken");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                listener.onFilm(accessToken);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        VolleyAbstract.getInstance(context).addToRequestQueue(request);
    }

    public static void fetchFilms(final Context context, String searchQuery, String accessToken, final DeoDungNuaV2.FilmListener<ArrayList<DeoDungNuaV2.Film>> listener) throws UnsupportedEncodingException {
        String url = String.format("%s/filmlist/&q=%s&accesstoken=%s",
                root,
                URLEncoder.encode(searchQuery, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ArrayList<DeoDungNuaV2.Film> films = new ArrayList<>();

                if (response.has("phim")) {
                    try {
                        JSONArray responsePhim = response.getJSONArray("phim");
                        for (int i = 0, l = responsePhim.length(); i < l; i++) {
                            JSONObject responsePhimOne = responsePhim.getJSONObject(i);
                            DeoDungNuaV2.Film film = new DeoDungNuaV2.Film();
                            film.id = responsePhimOne.getString("filmID");
                            film.name = responsePhimOne.getString("filmName");
                            film.thumb = responsePhimOne.getString("filmThumb");

                            films.add(film);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                listener.onFilm(films);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        VolleyAbstract.getInstance(context).addToRequestQueue(request);
    }

    public static void fetchEpisodes(final Context context, final DeoDungNuaV2.Film film, String accessToken, final DeoDungNuaV2.FilmListener<ArrayList<DeoDungNuaV2.Episode>> listener) throws UnsupportedEncodingException {
        String url = String.format("%s/filmdetail/&id=%s&accesstoken=%s",
                root,
                URLEncoder.encode(film.id, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ArrayList<DeoDungNuaV2.Episode> episodes = new ArrayList<>();

                if (response.has("phim")) {
                    try {
                        JSONArray responsePhim = response.getJSONArray("phim");
                        JSONObject responsePhimOne = responsePhim.getJSONObject(0);
                        if (film.id.equals(responsePhimOne.getString("filmID"))) {
                            JSONArray responseEpsList = responsePhimOne.getJSONArray("epsList");
                            for (int i = 0, l = responseEpsList.length(); i < l; i++) {
                                JSONObject responseEpisode = responseEpsList.getJSONObject(i);
                                DeoDungNuaV2.Episode episode = new DeoDungNuaV2.Episode();
                                episode.film = film;
                                episode.id = responseEpisode.getString("id");
                                episode.name = responseEpisode.getString("name");

                                episodes.add(episode);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                listener.onFilm(episodes);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        VolleyAbstract.getInstance(context).addToRequestQueue(request);
    }

    public static void fetchMedia(final Context context, final DeoDungNuaV2.Episode episode, String accessToken, final DeoDungNuaV2.FilmListener<DeoDungNuaV2.Media> listener) throws UnsupportedEncodingException {
        String url = String.format("http://pub.vn/api/android/&mov_id=%s&eps_id=%s&accesstoken=%s",
                URLEncoder.encode(episode.film.id, "utf-8"),
                URLEncoder.encode(episode.id, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                DeoDungNuaV2.Media completeMedia = null;

                if (response.has("movieURL")) {
                    try {
                        DeoDungNuaV2.Media media = new DeoDungNuaV2.Media();
                        media.episode = episode;
                        media.url = response.getString("movieURL");
//                        media.url = CorsProxy.buildUrl(media.url);

                        if (response.has("subtitleURL")) {
                            JSONObject responseSubtitles = response.getJSONObject("subtitleURL");
                            JSONArray names = responseSubtitles.names();
                            for (int i = 0, l = names.length(); i < l; i++) {
                                String name = names.getString(i);
                                JSONObject responseSubtitle = responseSubtitles.getJSONObject(name);
                                DeoDungNuaV2.Subtitle subtitle = new DeoDungNuaV2.Subtitle();
                                subtitle.languageCode = "vietnam".equals(name) ? "vi" : "en";
                                subtitle.languageName = responseSubtitle.getString("languageName");
                                subtitle.url = PubvnDecodeSrt.buildUrl(responseSubtitle.getJSONObject("subtitleData").getString("bottom"));

//                                media.subtitles.add(subtitle);
                            }
                        }

                        completeMedia = media;
                    } catch (JSONException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                if (completeMedia != null) {
                    listener.onFilm(completeMedia);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        VolleyAbstract.getInstance(context).addToRequestQueue(request);
    }

    public static String md5(String in) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(in.getBytes());
            byte[] a = digest.digest();
            int len = a.length;
            StringBuilder sb = new StringBuilder(len << 1);
            for (byte anA : a) {
                sb.append(Character.forDigit((anA & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(anA & 0x0f, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
