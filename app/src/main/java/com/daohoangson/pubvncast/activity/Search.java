package com.daohoangson.pubvncast.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.DeoDungNuaAndroid;
import com.daohoangson.pubvncast.networking.DeoDungNua;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class Search extends AppCompatActivity implements DeoDungNua.FilmListener<ArrayList<DeoDungNua.Film>> {

    public static final int REQUEST_CODE_LOGIN = 1;
    public static final String PREF_ACCESS_TOKEN = "accessToken";
    public static final String PREF_SEARCH_QUERIES = "searchQueries";

    private EditText mAccessToken;
    private EditText mSearchQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        mAccessToken = (EditText) findViewById(R.id.txtAccessToken);
        assert mAccessToken != null;
        mAccessToken.setText(preferences.getString(PREF_ACCESS_TOKEN, ""));
        mAccessToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(Search.this, Login.class);
                startActivityForResult(login, REQUEST_CODE_LOGIN);
            }
        });

        mSearchQuery = (EditText) findViewById(R.id.txtSearchQuery);
        assert mSearchQuery != null;
        Set<String> searchQueries = preferences.getStringSet(PREF_SEARCH_QUERIES, new LinkedHashSet<String>());
        final ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        if (searchQueries.size() > 0) {
            for (String searchQuery : searchQueries) {
                listAdapter.insert(searchQuery, 0);
            }
        }

        Button btnSearch = (Button) findViewById(R.id.btnSearch);
        assert btnSearch != null;
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String accessToken = mAccessToken.getText().toString();
                if (TextUtils.isEmpty(accessToken)) {
                    return;
                }

                String searchQuery = mSearchQuery.getText().toString();
                if (TextUtils.isEmpty(searchQuery)) {
                    return;
                }

                mSearchQuery.setText("");
                listAdapter.insert(searchQuery, 0);
                TreeSet<String> searchQueries = new TreeSet<>();
                for (int i = listAdapter.getCount() - 1; i >= 0; i--) {
                    searchQueries.add(listAdapter.getItem(i));
                }
                while (searchQueries.size() > 5) {
                    searchQueries.remove(searchQueries.last());
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_ACCESS_TOKEN, accessToken);
                editor.putStringSet(PREF_SEARCH_QUERIES, searchQueries);
                editor.apply();

                try {
                    DeoDungNuaAndroid.fetchFilms(Search.this, searchQuery, accessToken, Search.this);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        ListView lvSearchQueries = (ListView) findViewById(R.id.listSearchQueries);
        assert lvSearchQueries != null;
        lvSearchQueries.setAdapter(listAdapter);
        lvSearchQueries.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String searchQuery = listAdapter.getItem(position);
                mSearchQuery.setText(searchQuery);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_LOGIN:
                if (resultCode == RESULT_OK) {
                    String accessToken = data.getStringExtra(Login.RESULT_ACCESS_TOKEN);
                    mAccessToken.setText(accessToken);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onFilm(ArrayList<DeoDungNua.Film> data) {
        Intent filmList = new Intent(this, FilmList.class);
        filmList.putExtra(FilmList.INTENT_EXTRA_ACCESS_TOKEN, mAccessToken.getText().toString());
        filmList.putExtra(FilmList.INTENT_EXTRA_FILMS, data);
        startActivity(filmList);
    }
}
