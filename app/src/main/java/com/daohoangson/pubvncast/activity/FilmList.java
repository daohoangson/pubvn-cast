package com.daohoangson.pubvncast.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.DeoDungNuaAndroid;
import com.daohoangson.pubvncast.networking.DeoDungNuaV2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FilmList extends AppCompatActivity implements AdapterView.OnItemClickListener, DeoDungNuaV2.FilmListener<ArrayList<DeoDungNuaV2.Episode>> {

    public static final String INTENT_EXTRA_ACCESS_TOKEN = "accessToken";
    public static final String INTENT_EXTRA_FILMS = "films";

    private ListView mList;
    private FilmAdapter mAdapter;
    private String mAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mList = (ListView) findViewById(R.id.list);
        assert mList != null;
        mList.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();

        mAccessToken = intent.getStringExtra(INTENT_EXTRA_ACCESS_TOKEN);

        ArrayList<DeoDungNuaV2.Film> films = (ArrayList<DeoDungNuaV2.Film>) intent.getSerializableExtra(INTENT_EXTRA_FILMS);
        mAdapter = new FilmAdapter(this, android.R.layout.simple_list_item_1, films);
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeoDungNuaV2.Film film = mAdapter.getItem(position);
        if (film == null) {
            return;
        }

        if (TextUtils.isEmpty(mAccessToken)) {
            return;
        }

        try {
            DeoDungNuaAndroid.fetchEpisodes(this, film, mAccessToken, this);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFilm(ArrayList<DeoDungNuaV2.Episode> data) {
        Intent episodeList = new Intent(this, EpisodeList.class);
        episodeList.putExtra(EpisodeList.INTENT_EXTRA_ACCESS_TOKEN, mAccessToken);
        episodeList.putExtra(EpisodeList.INTENT_EXTRA_EPISODES, data);
        startActivity(episodeList);
    }

    private static class FilmAdapter extends ArrayAdapter<DeoDungNuaV2.Film> {
        public FilmAdapter(Context context, int resource, List<DeoDungNuaV2.Film> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }
}
