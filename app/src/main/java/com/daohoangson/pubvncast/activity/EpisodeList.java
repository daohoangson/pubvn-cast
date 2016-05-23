package com.daohoangson.pubvncast.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class EpisodeList extends AppCompatActivity implements AdapterView.OnItemClickListener, DeoDungNuaV2.FilmListener<DeoDungNuaV2.Media> {

    public static final String INTENT_EXTRA_ACCESS_TOKEN = "accessToken";
    public static final String INTENT_EXTRA_EPISODES = "episodes";

    private ListView mList;
    private EpisodeAdapter mAdapter;
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

        ArrayList<DeoDungNuaV2.Episode> episodes = (ArrayList<DeoDungNuaV2.Episode>) intent.getSerializableExtra(INTENT_EXTRA_EPISODES);
        mAdapter = new EpisodeAdapter(this, android.R.layout.simple_list_item_1, episodes);
        mList.setAdapter(mAdapter);

        if (mAdapter.getCount() > 0) {
            DeoDungNuaV2.Episode episode = mAdapter.getItem(0);
            setTitle(episode.film.name);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeoDungNuaV2.Episode episode = mAdapter.getItem(position);
        if (episode == null) {
            return;
        }

        if (TextUtils.isEmpty(mAccessToken)) {
            return;
        }

        try {
            DeoDungNuaAndroid.fetchMedia(this, episode, mAccessToken, this);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFilm(DeoDungNuaV2.Media data) {
        Intent player = new Intent(this, Player.class);
        player.putExtra(Player.INTENT_EXTRA_MEDIA, data);
        startActivity(player);
    }

    private static class EpisodeAdapter extends ArrayAdapter<DeoDungNuaV2.Episode> {
        public EpisodeAdapter(Context context, int resource, List<DeoDungNuaV2.Episode> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }
}
