package com.daohoangson.pubvncast.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daohoangson.pubvncast.R;
import com.daohoangson.pubvncast.networking.DeoDungNua;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Player extends AppCompatActivity {

    public static final String INTENT_EXTRA_MEDIA = "media";

    private TextView mMovie;
    private TextView mEpisode;
    private RadioGroup mSubtitles;
    private Button mPlay;
    private LinearLayout mPanePlaying;
    private TextView mElapsed;
    private SeekBar mSeekBar;
    private Handler mSeekBarHandler;
    private Runnable mSeekBarRunner;
    private DeoDungNua.Media mMedia;

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private Cast.Listener mCastClientListener;
    private List<MediaTrack> mMediaTracks;
    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mVideoIsLoaded;
    private boolean mIsPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mMovie = (TextView) findViewById(R.id.txtMovie);
        mEpisode = (TextView) findViewById(R.id.txtEpisode);

        mSubtitles = (RadioGroup) findViewById(R.id.rgSubtitles);
        mSubtitles.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mRemoteMediaPlayer == null
                        || mApiClient == null
                        || !mVideoIsLoaded) {
                    return;
                }

                long[] trackIds = getActiveTrackIds();
                mRemoteMediaPlayer.setActiveMediaTracks(mApiClient, trackIds);
            }
        });

        mPlay = (Button) findViewById(R.id.btnPlay);
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mVideoIsLoaded) {
                    startVideo();
                } else {
                    if (mIsPlaying) {
                        mRemoteMediaPlayer.pause(mApiClient);
                    } else {
                        mRemoteMediaPlayer.play(mApiClient);
                    }

                    updateViews();
                }
            }
        });

        mPanePlaying = (LinearLayout) findViewById(R.id.panePlaying);
        mElapsed = (TextView) findViewById(R.id.txtElapsed);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser
                        && mVideoIsLoaded
                        && mRemoteMediaPlayer != null
                        && mApiClient != null) {
                    mRemoteMediaPlayer.seek(mApiClient, progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBarHandler = new Handler();
        mSeekBarRunner = new Runnable() {
            @Override
            public void run() {
                if (mRemoteMediaPlayer == null
                        || mSeekBar == null
                        || mSeekBarHandler == null) {
                    return;
                }

                if (!mIsPlaying) {
                    return;
                }

                int elapsed = (int) (mRemoteMediaPlayer.getApproximateStreamPosition() / 1000);

                int seconds = elapsed % 60;
                int minutes = (elapsed - seconds) / 60;
                int hours = (elapsed - seconds - minutes * 60) / 3600;
                mElapsed.setText(String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds));

                mSeekBar.setProgress(elapsed);
                mSeekBar.refreshDrawableState();

                mSeekBarHandler.postDelayed(mSeekBarRunner, 1000);
            }
        };

        initMediaRouter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.player, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);

        Intent intent = getIntent();
        mMedia = (DeoDungNua.Media) intent.getSerializableExtra(INTENT_EXTRA_MEDIA);
        renderSubtitles();
        updateViews();
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    private void initMediaRouter() {
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        String applicationId = getString(R.string.cast_app_id);
        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(applicationId))
                .build();
        mMediaRouterCallback = new MediaRouterCallback();
    }

    private void initCastClientListener() {
        mCastClientListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
            }

            @Override
            public void onVolumeChanged() {
            }

            @Override
            public void onApplicationDisconnected(int statusCode) {
                teardown();
            }
        };
    }

    private void initRemoteMediaPlayer() {
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                mIsPlaying = mediaStatus.getPlayerState() == MediaStatus.PLAYER_STATE_PLAYING;

                updateViews();

                if (mSeekBarRunner != null) {
                    mSeekBarRunner.run();
                }
            }
        });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
            }
        });

        updateViews();
    }

    private void launchReceiver() {
        Cast.CastOptions.Builder apiOptionsBuilder = new Cast.CastOptions
                .Builder(mSelectedDevice, mCastClientListener);

        ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
        ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptionsBuilder.build())
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();

        mApiClient.connect();
    }

    private long[] getActiveTrackIds() {
        if (mSubtitles == null
                || mMediaTracks == null) {
            return new long[0];
        }

        DeoDungNua.Subtitle checkedSubtitle = null;
        int checkedSubtitleRbId = mSubtitles.getCheckedRadioButtonId();
        if (checkedSubtitleRbId != -1) {
            RadioButton checkedSubtitleRb = (RadioButton) findViewById(checkedSubtitleRbId);
            assert checkedSubtitleRb != null;
            Object checkedSubtitleRbTag = checkedSubtitleRb.getTag();
            if (checkedSubtitleRbTag != null
                    && checkedSubtitleRbTag instanceof DeoDungNua.Subtitle) {
                checkedSubtitle = (DeoDungNua.Subtitle) checkedSubtitleRbTag;
            }
        }

        List<MediaTrack> activeTracks = new ArrayList<>();
        for (MediaTrack mediaTrack : mMediaTracks) {
            if (checkedSubtitle != null
                    && mediaTrack.getContentId().equals(checkedSubtitle.url)) {
                activeTracks.add(mediaTrack);
            }
        }

        long[] activeTrackIds = new long[activeTracks.size()];
        for (int i = 0, l = activeTracks.size(); i < l; i++) {
            activeTrackIds[i] = activeTracks.get(i).getId();
        }

        return activeTrackIds;
    }

    private void startVideo() {
        if (mMedia == null
                || TextUtils.isEmpty(mMedia.url)
                || mRemoteMediaPlayer == null) {
            return;
        }

        mMediaTracks = new ArrayList<>();
        for (int i = 0, l = mMedia.subtitles.size(); i < l; i++) {
            DeoDungNua.Subtitle subtitle = mMedia.subtitles.get(i);
            MediaTrack subtitleTrack = new MediaTrack.Builder(i + 1, MediaTrack.TYPE_TEXT)
                    .setLanguage(subtitle.languageCode)
                    .setName(subtitle.languageName)
                    .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                    .setContentId(subtitle.url)
                    .build();
            mMediaTracks.add(subtitleTrack);
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(mMedia.url)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMediaTracks(mMediaTracks)
                .build();

        long[] activeTrackIds = getActiveTrackIds();

        mRemoteMediaPlayer.load(mApiClient, mediaInfo, true, 0, activeTrackIds, null)
                .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                    @Override
                    public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                        if (mediaChannelResult.getStatus().isSuccess()) {
                            mVideoIsLoaded = true;
                            updateViews();
                        }
                    }
                });
    }

    private void reconnectChannels(Bundle hint) {
        if ((hint != null) && hint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
            //Log.e( TAG, "App is no longer running" );
            teardown();
        } else {
            try {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void teardown() {
        if (mApiClient != null) {
            if (mApplicationStarted) {
                try {
                    Cast.CastApi.stopApplication(mApiClient);
                    if (mRemoteMediaPlayer != null) {
                        Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace());
                        mRemoteMediaPlayer = null;
                    }
                } catch (IOException e) {
                    //Log.e( TAG, "Exception while removing application " + e );
                }
                mApplicationStarted = false;
            }
            if (mApiClient.isConnected())
                mApiClient.disconnect();
            mApiClient = null;
        }
        mSelectedDevice = null;
        mVideoIsLoaded = false;

        updateViews();
    }

    private void renderSubtitles() {
        if (mMedia.subtitles.size() > 0) {
            mSubtitles.removeAllViews();

            RadioButton rb = new RadioButton(this);
            rb.setId(0);
            rb.setText(R.string.no_subtitle);
            rb.setChecked(true);
            mSubtitles.addView(rb);

            for (int i = 0, l = mMedia.subtitles.size(); i < l; i++) {
                DeoDungNua.Subtitle subtitle = mMedia.subtitles.get(i);
                rb = new RadioButton(this);
                rb.setId(1 + i);
                rb.setText(subtitle.languageName);
                rb.setTag(subtitle);

                mSubtitles.addView(rb);
            }
        } else {
            mSubtitles.setVisibility(View.GONE);
        }
    }

    private void updateViews() {
        if (mSelectedDevice != null) {
            setTitle(mSelectedDevice.getFriendlyName());
        } else {
            setTitle(getString(R.string.app_name));
        }

        if (mMedia != null) {
            mMovie.setText(mMedia.episode.film.name);
            mEpisode.setText(mMedia.episode.name);

            if (mRemoteMediaPlayer != null) {
                if (!mVideoIsLoaded) {
                    mPlay.setText(getString(R.string.play_video));
                    mPanePlaying.setVisibility(View.GONE);
                } else if (mIsPlaying) {
                    mPlay.setText(getString(R.string.pause_video));
                    mSeekBar.setMax((int) (mRemoteMediaPlayer.getStreamDuration() / 1000));
                    mPanePlaying.setVisibility(View.VISIBLE);
                } else {
                    mPlay.setText(getString(R.string.resume_video));
                    mPanePlaying.setVisibility(View.GONE);
                }
                mPlay.setEnabled(true);
            } else {
                mPlay.setText(getString(R.string.play_video));
                mPlay.setEnabled(false);
                mPanePlaying.setVisibility(View.GONE);
            }
        } else {
            mMovie.setText("");
            mEpisode.setText("");
            mPlay.setText(getString(R.string.play_video));
            mPlay.setEnabled(false);
            mPanePlaying.setVisibility(View.GONE);
        }
    }

    private class MediaRouterCallback extends MediaRouter.Callback {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            initCastClientListener();
            initRemoteMediaPlayer();

            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            launchReceiver();

            updateViews();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            teardown();
            mSelectedDevice = null;
            mVideoIsLoaded = false;

            updateViews();
        }
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {

        @Override
        public void onConnected(Bundle hint) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;
                reconnectChannels(hint);
            } else {
                try {
                    Cast.CastApi.launchApplication(mApiClient, getString(R.string.cast_app_id))
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(
                                                @NonNull Cast.ApplicationConnectionResult applicationConnectionResult) {
                                            Status status = applicationConnectionResult.getStatus();
                                            if (status.isSuccess()) {
                                                mApplicationStarted = true;
                                                reconnectChannels(null);
                                            }
                                        }
                                    }
                            );
                } catch (Exception ignored) {

                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            teardown();
        }
    }
}
