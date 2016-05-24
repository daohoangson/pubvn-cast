package com.daohoangson.pubvncast.activity;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;

import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.VolleyAbstract;

public abstract class Networking extends AppCompatActivity {

    private ProgressDialog mProgressDialog;

    public void showProgressDialog(String explain) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this,
                    getString(R.string.please_wait), explain);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        VolleyAbstract.getInstance(this).getRequestQueue().cancelAll(this);

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
}
