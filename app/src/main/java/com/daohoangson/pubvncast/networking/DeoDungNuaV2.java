package com.daohoangson.pubvncast.networking;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class DeoDungNuaV2 {

    public static final String root = "http://deodungnua.bup.vn/puse/v2/json_api.php";

    @SuppressWarnings("unused")
    public static void fetchFilms(final Context context, String searchQuery, String accessToken, final FilmListener<ArrayList<Film>> listener) throws UnsupportedEncodingException {
        String url = String.format("%s?action=filmlist&q=%s&accesstoken=%s",
                root,
                URLEncoder.encode(searchQuery, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ArrayList<Film> films = new ArrayList<>();

                if (response.has("phim")) {
                    try {
                        JSONArray responsePhim = response.getJSONArray("phim");
                        for (int i = 0, l = responsePhim.length(); i < l; i++) {
                            JSONObject responsePhimOne = responsePhim.getJSONObject(i);
                            Film film = new Film();
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

    @SuppressWarnings("unused")
    public static void fetchEpisodes(final Context context, final Film film, String accessToken, final FilmListener<ArrayList<Episode>> listener) throws UnsupportedEncodingException {
        String url = String.format("%s?action=filmdetail&id=%s&accesstoken=%s",
                root,
                URLEncoder.encode(film.id, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ArrayList<Episode> episodes = new ArrayList<>();

                if (response.has("phim")) {
                    try {
                        JSONArray responsePhim = response.getJSONArray("phim");
                        JSONObject responsePhimOne = responsePhim.getJSONObject(0);
                        if (film.id.equals(responsePhimOne.getString("filmID"))) {
                            JSONArray responseEpsList = responsePhimOne.getJSONArray("epsList");
                            for (int i = 0, l = responseEpsList.length(); i < l; i++) {
                                JSONObject responseEpisode = responseEpsList.getJSONObject(i);
                                Episode episode = new Episode();
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

    @SuppressWarnings("unused")
    public static void fetchMedia(final Context context, final Episode episode, String accessToken, final FilmListener<Media> listener) throws UnsupportedEncodingException {
        String url = String.format("%s?action=filmmedia&movieid=%s&epsid=%s&accesstoken=%s",
                root,
                URLEncoder.encode(episode.film.id, "utf-8"),
                URLEncoder.encode(episode.id, "utf-8"),
                URLEncoder.encode(accessToken, "utf-8"));
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Media completeMedia = null;

                if (response.has("movieURL")) {
                    try {
                        Media media = new Media();
                        media.episode = episode;
                        media.url = response.getString("movieURL");

                        completeMedia = media;
                    } catch (JSONException e) {
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

    public interface FilmListener<T> {
        void onFilm(T data);
    }

    public static class Film implements Serializable {
        public String id;
        public String name;
        public String thumb;

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Episode implements Serializable {
        public Film film;
        public String id;
        public String name;

        @Override
        public String toString() {
            return name;
        }
    }

    public static class Media implements Serializable {
        public Episode episode;
        public String url;
        public ArrayList<Subtitle> subtitles = new ArrayList<>();
    }

    public static class Subtitle implements Serializable {
        public String languageCode;
        public String languageName;
        public String url;
    }
}
