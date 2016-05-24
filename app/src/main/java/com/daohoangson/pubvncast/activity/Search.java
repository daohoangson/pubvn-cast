package com.daohoangson.pubvncast.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.DeoDungNuaAndroid;
import com.daohoangson.pubvncast.networking.DeoDungNua;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Search extends AppCompatActivity implements DeoDungNua.FilmListener<ArrayList<DeoDungNua.Film>> {

    public static final int REQUEST_CODE_LOGIN = 1;

    private EditText mAccessToken;
    private EditText mSearchQuery;
    private Button mSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAccessToken = (EditText) findViewById(R.id.txtAccessToken);
        assert mAccessToken != null;
        mAccessToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(Search.this, Login.class);
                startActivityForResult(login, REQUEST_CODE_LOGIN);
            }
        });

        mSearchQuery = (EditText) findViewById(R.id.txtSearchQuery);
        assert mSearchQuery != null;
        mSearchQuery.setText("Game Of Thrones");

        mSearch = (Button) findViewById(R.id.btnSearch);
        assert mSearch != null;
        mSearch.setOnClickListener(new View.OnClickListener() {
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

                try {
                    DeoDungNuaAndroid.fetchFilms(Search.this, searchQuery, accessToken, Search.this);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_LOGIN:
                if (resultCode == RESULT_OK) {
                    mAccessToken.setText(data.getStringExtra(Login.RESULT_ACCESS_TOKEN));
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
