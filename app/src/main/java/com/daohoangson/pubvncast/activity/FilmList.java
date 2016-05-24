package com.daohoangson.pubvncast.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.DeoDungNua;
import com.daohoangson.pubvncast.networking.DeoDungNuaAndroid;
import com.daohoangson.pubvncast.networking.VolleyAbstract;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class FilmList extends Networking implements AdapterView.OnItemClickListener, DeoDungNua.FilmListener<ArrayList<DeoDungNua.Episode>> {

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

        @SuppressWarnings("unchecked")
        ArrayList<DeoDungNua.Film> films = (ArrayList<DeoDungNua.Film>) intent.getSerializableExtra(INTENT_EXTRA_FILMS);
        mAdapter = new FilmAdapter(this, R.layout.list_item_text_and_image, films);
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DeoDungNua.Film film = mAdapter.getItem(position);
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
    public void onFilm(ArrayList<DeoDungNua.Episode> data) {
        Intent episodeList = new Intent(this, EpisodeList.class);
        episodeList.putExtra(EpisodeList.INTENT_EXTRA_ACCESS_TOKEN, mAccessToken);
        episodeList.putExtra(EpisodeList.INTENT_EXTRA_EPISODES, data);
        startActivity(episodeList);
    }

    private static class FilmAdapter extends ArrayAdapter<DeoDungNua.Film> {

        private Networking mActivity;
        private LayoutInflater mInflater;

        public FilmAdapter(Networking activity, int resource, List<DeoDungNua.Film> objects) {
            super(activity, resource, objects);

            mActivity = activity;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            DeoDungNua.Film film = getItem(position);

            if (convertView == null) {
                if (mInflater == null) {
                    mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                }

                convertView = mInflater.inflate(R.layout.list_item_text_and_image, null);
                viewHolder = new ViewHolder();
                viewHolder.networkImageView = (NetworkImageView) convertView.findViewById(R.id.imageView);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.textView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.networkImageView.setImageUrl(film.thumb, VolleyAbstract.getInstance(mActivity).getImageLoader());
            viewHolder.textView.setText(film.toString());

            return convertView;
        }

        private static class ViewHolder {
            NetworkImageView networkImageView;
            TextView textView;
        }
    }


}
