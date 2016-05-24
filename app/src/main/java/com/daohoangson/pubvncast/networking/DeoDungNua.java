package com.daohoangson.pubvncast.networking;

import java.io.Serializable;
import java.util.ArrayList;

public class DeoDungNua {
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
