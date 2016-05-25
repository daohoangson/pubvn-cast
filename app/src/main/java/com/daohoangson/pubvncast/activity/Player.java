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
import com.google.android.gms.cast.LaunchOptions;
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

public class Player extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    public static final String INTENT_EXTRA_MEDIA = "media";

    private TextView mMovie;
    private TextView mEpisode;
    private RadioGroup mMediaTracks;
    private Button mControl;
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
    private boolean mWaitingForReconnect = false;
    private ApplicationState mApplicationState = ApplicationState.DISCONNECTED;
    private boolean mVideoIsLoaded = false;
    private boolean mWaitingForMetadata = false;
    private boolean mIsPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        mMovie = (TextView) findViewById(R.id.txtMovie);
        mEpisode = (TextView) findViewById(R.id.txtEpisode);

        mMediaTracks = (RadioGroup) findViewById(R.id.rgMediaTracks);

        mControl = (Button) findViewById(R.id.btnControl);
        mControl.setOnClickListener(new View.OnClickListener() {
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
        mControl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startVideo();
                return true;
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
                int minutes = ((elapsed - seconds) / 60) % 60;
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
        prepareMediaTracks();
        updateViews();
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

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
                if (mediaStatus == null) {
                    return;
                }

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
                MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                if (mediaInfo == null) {
                    return;
                }

                if (mediaInfo.getContentId() != null) {
                    mVideoIsLoaded = true;
                }

                if (mWaitingForMetadata) {
                    prepareMediaTracks();
                    updateViews();
                    mWaitingForMetadata = false;
                }
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
        if (mMediaTracks == null) {
            return new long[0];
        }

        List<MediaTrack> activeTracks = new ArrayList<>();
        MediaTrack checkedMediaTrack = null;
        int checkedRbId = mMediaTracks.getCheckedRadioButtonId();
        RadioButton checkedRadioButton = (RadioButton) findViewById(checkedRbId);
        if (checkedRadioButton != null) {
            Object checkedTag = checkedRadioButton.getTag();
            if (checkedTag != null) {
                if (checkedTag instanceof MediaTrack) {
                    checkedMediaTrack = (MediaTrack) checkedTag;
                }
            }
        }

        if (mApplicationState == ApplicationState.JOINED
                && checkedMediaTrack != null) {
            activeTracks.add(checkedMediaTrack);
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

        mApplicationState = ApplicationState.STARTED;
        prepareMediaTracks();

        ArrayList<MediaTrack> mediaTracks = new ArrayList<>();
        for (int i = 0, l = mMediaTracks.getChildCount(); i < l; i++) {
            View view = mMediaTracks.getChildAt(i);
            if (view instanceof RadioButton) {
                RadioButton rb = (RadioButton) view;
                Object tag = rb.getTag();
                if (tag instanceof MediaTrack) {
                    mediaTracks.add((MediaTrack) tag);
                }
            }
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(mMedia.url)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMediaTracks(mediaTracks)
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
            if (mApplicationState != ApplicationState.DISCONNECTED) {
                Cast.CastApi.stopApplication(mApiClient);
                if (mRemoteMediaPlayer != null) {
                    try {
                        Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mRemoteMediaPlayer = null;
                }
                mApplicationState = ApplicationState.DISCONNECTED;
            }
            if (mApiClient.isConnected())
                mApiClient.disconnect();
            mApiClient = null;
        }
        mSelectedDevice = null;
        mVideoIsLoaded = false;

        updateViews();
    }

    private void prepareMediaTracks() {
        if (mApplicationState == ApplicationState.JOINED
                && mRemoteMediaPlayer != null) {
            MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
            if (mediaInfo != null) {
                mMediaTracks.setOnCheckedChangeListener(null);
                mMediaTracks.removeAllViews();

                RadioButton rb = new RadioButton(this);
                rb.setId(0);
                rb.setText(R.string.no_subtitle);
                rb.setChecked(true);
                mMediaTracks.addView(rb);

                for (MediaTrack mediaTrack : mediaInfo.getMediaTracks()) {
                    rb = new RadioButton(this);
                    rb.setId(1 + (int) mediaTrack.getId());
                    rb.setText(mediaTrack.getContentId());
                    rb.setTag(mediaTrack);

                    mMediaTracks.addView(rb);
                }

                mMediaTracks.setOnCheckedChangeListener(this);
                mMediaTracks.setVisibility(View.VISIBLE);

                return;
            }
        }

        if (mApplicationState != ApplicationState.JOINED
                && mMedia != null
                && mMedia.subtitles.size() > 0) {
            mMediaTracks.setOnCheckedChangeListener(null);
            mMediaTracks.removeAllViews();

            RadioButton rb = new RadioButton(this);
            rb.setId(0);
            rb.setText(R.string.no_subtitle);
            rb.setChecked(true);
            mMediaTracks.addView(rb);

            for (int i = 0, l = mMedia.subtitles.size(); i < l; i++) {
                DeoDungNua.Subtitle subtitle = mMedia.subtitles.get(i);
                MediaTrack subtitleTrack = new MediaTrack.Builder(i + 1, MediaTrack.TYPE_TEXT)
                        .setLanguage(subtitle.languageCode)
                        .setName(subtitle.languageName)
                        .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                        .setContentId(subtitle.url)
                        .build();

                rb = new RadioButton(this);
                rb.setId(1 + i);
                rb.setText(subtitleTrack.getName());
                rb.setTag(subtitleTrack);

                mMediaTracks.addView(rb);
            }

            mMediaTracks.setOnCheckedChangeListener(this);
            mMediaTracks.setVisibility(View.VISIBLE);

            return;
        }

        mMediaTracks.setVisibility(View.GONE);
    }

    private void updateViews() {
        if (mSelectedDevice != null) {
            setTitle(mSelectedDevice.getFriendlyName());
        } else {
            setTitle(getString(R.string.app_name));
        }

        String movieName = null;
        String episodeName = null;
        switch (mApplicationState) {
            case JOINED:
                if (mRemoteMediaPlayer != null) {
                    MediaInfo mediaInfo = mRemoteMediaPlayer.getMediaInfo();
                    if (mediaInfo != null) {
                        movieName = mediaInfo.getContentType();
                        episodeName = mediaInfo.getContentId();
                    }
                }
                break;
            default:
                if (mMedia != null) {
                    movieName = mMedia.episode.film.name;
                    episodeName = mMedia.episode.name;
                }
        }

        if (movieName != null && episodeName != null) {
            mMovie.setText(movieName);
            mEpisode.setText(episodeName);

            if (mRemoteMediaPlayer != null) {
                if (!mVideoIsLoaded) {
                    mControl.setText(getString(R.string.play_video));
                    mPanePlaying.setVisibility(View.GONE);
                } else if (mIsPlaying) {
                    mControl.setText(getString(R.string.pause_video));
                    mSeekBar.setMax((int) (mRemoteMediaPlayer.getStreamDuration() / 1000));
                    mPanePlaying.setVisibility(View.VISIBLE);
                } else {
                    mControl.setText(getString(R.string.resume_video));
                    mPanePlaying.setVisibility(View.GONE);
                }
                mControl.setEnabled(true);
            } else {
                mControl.setText(getString(R.string.play_video));
                mControl.setEnabled(false);
                mPanePlaying.setVisibility(View.GONE);
            }
        } else {
            mMovie.setText("");
            mEpisode.setText("");
            mControl.setText(getString(R.string.play_video));
            mControl.setEnabled(false);
            mPanePlaying.setVisibility(View.GONE);
        }
    }

    private enum ApplicationState {
        DISCONNECTED,
        STARTED,
        JOINED
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
                LaunchOptions launchOptions = new LaunchOptions.Builder()
                        .setRelaunchIfRunning(false)
                        .build();

                Cast.CastApi.launchApplication(mApiClient, getString(R.string.cast_app_id), launchOptions)
                        .setResultCallback(
                                new ResultCallback<Cast.ApplicationConnectionResult>() {
                                    @Override
                                    public void onResult(
                                            @NonNull Cast.ApplicationConnectionResult applicationConnectionResult) {
                                        Status status = applicationConnectionResult.getStatus();
                                        if (status.isSuccess()) {
                                            if (Cast.CastApi.getApplicationStatus(mApiClient) != null) {
                                                mApplicationState = ApplicationState.JOINED;
                                                mWaitingForMetadata = true;
                                                mRemoteMediaPlayer.requestStatus(mApiClient);
                                            } else {
                                                mApplicationState = ApplicationState.STARTED;
                                            }

                                            reconnectChannels(null);
                                        }
                                    }
                                }
                        );
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
