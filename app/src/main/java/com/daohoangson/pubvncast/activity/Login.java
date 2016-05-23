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
import com.daohoangson.pubvncast.networking.DeoDungNuaV2;

import java.io.UnsupportedEncodingException;

public class Login extends AppCompatActivity implements DeoDungNuaV2.FilmListener<String> {

    public static final String RESULT_ACCESS_TOKEN = "accessToken";
    public static final String PREF_USERNAME = "username";
    public static final String PREF_PASSWORD_MD5 = "passwordMd5";

    private EditText mUsername;
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String defaultUsername = preferences.getString(PREF_USERNAME, "");
        final String defaultPassword = preferences.getString(PREF_PASSWORD_MD5, "");

        mUsername = (EditText) findViewById(R.id.txtUsername);
        assert mUsername != null;
        mUsername.setText(defaultUsername);

        mPassword = (EditText) findViewById(R.id.txtPassword);
        assert mPassword != null;
        mPassword.setText(defaultPassword);

        Button login = (Button) findViewById(R.id.btnLogin);
        assert login != null;
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mUsername.getText().toString();
                if (TextUtils.isEmpty(username)) {
                    return;
                }

                String password = mPassword.getText().toString();
                if (TextUtils.isEmpty(password)) {
                    return;
                }
                String passwordMd5;
                if (password.equals(defaultPassword)) {
                    // user didn't change the default password (md5 version)
                    passwordMd5 = password;
                } else {
                    passwordMd5 = DeoDungNuaAndroid.md5(password);
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PREF_USERNAME, username);
                editor.putString(PREF_PASSWORD_MD5, passwordMd5);
                editor.apply();

                try {
                    DeoDungNuaAndroid.login(Login.this, username, passwordMd5, Login.this);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onFilm(String data) {
        Intent result = new Intent();
        result.putExtra(RESULT_ACCESS_TOKEN, data);
        setResult(RESULT_OK, result);
        finish();
    }
}
