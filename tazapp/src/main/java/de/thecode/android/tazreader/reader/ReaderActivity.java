package de.thecode.android.tazreader.reader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.listener.DialogButtonListener;
import de.mateware.dialog.listener.DialogDismissListener;
import de.mateware.snacky.Snacky;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.audio.AudioItem;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.HelpDialog;
import de.thecode.android.tazreader.notifications.NotificationUtils;
import de.thecode.android.tazreader.reader.article.ArticleFragment;
import de.thecode.android.tazreader.reader.article.TopLinkFragment;
import de.thecode.android.tazreader.reader.page.PagesFragment;
import de.thecode.android.tazreader.reader.pagetoc.PageTocFragment;
import de.thecode.android.tazreader.reader.usertoc.UserTocFragment;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.BaseActivity;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.TintHelper;

import org.json.JSONArray;

import java.io.File;
import java.util.WeakHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

@SuppressLint("RtlHardcoded")
public class ReaderActivity extends BaseActivity
        implements SettingsDialog.SettingsDialogCallback, DialogButtonListener, DialogDismissListener {

    private AudioManager audioManager;
    private String       bookId;
//    private String       resourceKey;

    public enum THEMES {
        normal("bgColorNormal"), sepia("bgColorSepia"), night("bgColorNight");

        private String bgColorName;

        THEMES(String bgColorName) {
            this.bgColorName = bgColorName;
        }

        public String getBgColorName() {
            return bgColorName;
        }
    }

    public enum DIRECTIONS {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    private static final String TAG_FRAGMENT_INDEX          = "IndexFragment";
    private static final String TAG_FRAGMENT_PAGEINDEX      = "PageIndexFragment";
    public static final  String TAG_FRAGMENT_DIALOG_SETTING = "settingsDialog";
    public static final  String TAG_DIALOG_TTS_ERROR        = "ttsError";
    //    public static final  String KEY_EXTRA_PAPER_ID          = "paperId";
//    public static final  String KEY_EXTRA_RESOURCE_KEY      = "resourceKey";
    public static final  String KEY_EXTRA_BOOK_ID           = "bookId";

    DrawerLayout     mDrawerLayout;
    View             mDrawerLayoutIndex;
    View             mDrawerLayoutPageIndex;
    FrameLayout      mContentFrame;
    ProgressBar      mLoadingProgress;
    ConstraintLayout playerLayout;
    ImageView        playerButtonStop;
    ImageView        playerButtonPause;
    ImageView        playerButtonPausePlay;
    ImageView        playerButtonRewind30;
    ProgressBar        playerWait;
    TextView playerDuration;
    TextView playerPosition;
    SeekBar playerSeekBar;
    boolean playSeekbarChanging = false;

    FragmentManager mFragmentManager;
    StorageManager  mStorage;


    UserTocFragment         mUserTocFragment;
    PageTocFragment         mPageTocFragment;
    AbstractContentFragment mContentFragment;


    ReaderViewModel      readerViewModel;
    ReaderTTSViewModel   ttsViewModel;
    ReaderAudioViewModel audioViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Orientation.setActivityOrientationFromPrefs(this);

        bookId = getIntent().getStringExtra(KEY_EXTRA_BOOK_ID);
        if (TextUtils.isEmpty(bookId)) throw new IllegalStateException("Activity Reader has to be called with extra BookID");
//        resourceKey = getIntent().getStringExtra(KEY_EXTRA_RESOURCE_KEY);
//        if (TextUtils.isEmpty(resourceKey))
//            throw new IllegalStateException("Activity Reader has to be called with extra Resource Key");
//        long paperId = getIntent().getLongExtra(KEY_EXTRA_PAPER_ID, -1L);
//        if (paperId == -1L) throw new IllegalStateException("Activity Reader has to be called with extra PaperId");


        mStorage = StorageManager.getInstance(this);

        NotificationUtils.getInstance(this)
                         .removeDownloadNotification(bookId);

        setContentView(R.layout.activity_reader);


        setBackgroundColor(onGetBackgroundColor(TazSettings.getInstance(this)
                                                           .getPrefString(TazSettings.PREFKEY.THEME, "normal")));

        playerLayout = findViewById(R.id.player_layout);
        playerLayout.setVisibility(View.GONE);
        playerButtonPausePlay =findViewById(R.id.play_button);
        playerButtonStop =findViewById(R.id.stop_button);
        playerButtonPause =findViewById(R.id.pause_button);
        playerButtonRewind30 =findViewById(R.id.rev_button);
        playerWait =findViewById(R.id.wait_progress);
        playerDuration = findViewById(R.id.duration);
        playerPosition = findViewById(R.id.position);
        playerSeekBar = findViewById(R.id.player_seekbar);

        playerButtonStop.setOnClickListener(v -> audioViewModel.stopPlaying());
        playerButtonPausePlay.setOnClickListener(v -> audioViewModel.pauseOrResume());
        playerButtonPause.setOnClickListener(v -> audioViewModel.pauseOrResume());
        playerButtonRewind30.setOnClickListener(v -> audioViewModel.rewind30Seconds());
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    audioViewModel.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                playSeekbarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playSeekbarChanging = false;
            }
        });


        readerViewModel = ViewModelProviders.of(this, ReaderViewModel.createFactory(getApplication(), bookId))
                                            .get(ReaderViewModel.class);
        ttsViewModel = ViewModelProviders.of(this)
                                         .get(ReaderTTSViewModel.class);
        audioViewModel = ViewModelProviders.of(this)
                                           .get(ReaderAudioViewModel.class);


        mLoadingProgress = findViewById(R.id.loading);
        mLoadingProgress.setVisibility(View.VISIBLE);


        mDrawerLayoutPageIndex = findViewById(R.id.right_drawer);
        DrawerLayout.LayoutParams pageIndexLayoutParams = (DrawerLayout.LayoutParams) mDrawerLayoutPageIndex.getLayoutParams();
        pageIndexLayoutParams.width = getResources().getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) + (getResources().getDimensionPixelSize(
                R.dimen.pageindex_padding) * 2);
        mDrawerLayoutPageIndex.setLayoutParams(pageIndexLayoutParams);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayoutIndex = findViewById(R.id.left_drawer);
        mContentFrame = findViewById(R.id.content_frame);

        mFragmentManager = getSupportFragmentManager();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        retainTtsFragment = ReaderTtsFragment.createOrRetainDataFragment(getSupportFragmentManager(), ReaderTtsFragment.class);
//        retainTtsFragment.setCallback(this);
//        if (retainTtsFragment.getTtsState() == ReaderTtsFragment.TTS.PLAYING) ttsPreparePlayingInActivity();

        mContentFragment = (AbstractContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        loadIndexFragment();
        loadPageIndexFragment();


        readerViewModel.getCurrentKeyLiveData()
                       .observe(this, new Observer<ITocItem>() {
                           @Override
                           public void onChanged(@Nullable ITocItem iTocItem) {
                               mLoadingProgress.setVisibility(View.GONE);
                               if (mContentFragment == null && iTocItem != null) {
                                   loadContentFragment(iTocItem.getKey());
                               }
                           }
                       });

        ttsViewModel.getLiveTtsState()
                    .observe(this, new Observer<ReaderTTSViewModel.TTS>() {
                        @Override
                        public void onChanged(@Nullable ReaderTTSViewModel.TTS ttsState) {
                            Timber.d("new tts state: %s", ttsState);
                            switch (ttsState) {
                                case IDLE:
                                    audioManager.abandonAudioFocus(ttsViewModel.getAudioFocusChangeListener());
                                    break;
                                case PAUSED:
                                    audioManager.abandonAudioFocus(ttsViewModel.getAudioFocusChangeListener());
                                    showTtsSnackbar(getString(R.string.toast_tts_paused),
                                                    getString(R.string.toast_tts_action_restart),
                                                    new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            ttsViewModel.stopTts();
                                                            if (ttsPreparePlayingInActivity()) {
                                                                ttsViewModel.restartTts();
                                                            }
                                                        }
                                                    });

                                    break;
                            }
                        }
                    });

        ttsViewModel.getLiveTtsError()
                    .observe(this, new Observer<ReaderTTSViewModel.TTSERROR>() {
                        @Override
                        public void onChanged(@Nullable ReaderTTSViewModel.TTSERROR ttserror) {
                            if (ttserror != null) {
                                Timber.w("error: %s", ttserror);
                                StringBuilder message = new StringBuilder(getString(R.string.dialog_tts_error));
                                switch (ttserror) {
                                    case LANG_MISSING_DATA:
                                        message.append(" ")
                                               .append(getString(R.string.dialog_tts_error_lang_missing_data));
                                        break;
                                    case LANG_NOT_SUPPORTED:
                                        message.append(" ")
                                               .append(getString(R.string.dialog_tts_error_lang_not_supported));
                                        break;
                                }
                                new Dialog.Builder().setMessage(message.toString())
                                                    .setNeutralButton(R.string.dialog_tts_error_settings)
                                                    .setPositiveButton()
                                                    .buildSupport()
                                                    .show(getSupportFragmentManager(), TAG_DIALOG_TTS_ERROR);

                            }
                        }
                    });

        audioViewModel.getCurrentAudioItemLiveData()
                      .observe(this, audioItem -> {
                          playerSeekBar.setMax(audioItem != null ? audioItem.getDuration(): 0);
                          playerSeekBar.setProgress(audioItem != null ? audioItem.getResumePosition(): 0);
                          playerDuration.setText(audioItem != null ? ReaderAudioViewModel.Companion.millisToTimeString(audioItem.getDuration()) : "-");
                          playerLayout.setVisibility(audioItem != null ? View.VISIBLE : View.GONE);
                          if (audioItem != null) {
                              ((TextView) playerLayout.findViewById(R.id.title)).setText(audioItem.getTitle());
                              ((TextView) playerLayout.findViewById(R.id.source)).setText(audioItem.getSource());

                          }
                      });

        audioViewModel.getCurrentPositionLiveData().observe(this, position -> {

            Timber.d("position! %d",position);
            playerPosition.setText(ReaderAudioViewModel.Companion.millisToTimeString(position));
            if (!playSeekbarChanging)
            playerSeekBar.setProgress(position);
        });

        audioViewModel.getCurrentStateLiveData().observe(this, state -> {
            switch (state) {
                case LOADING:
                    playerButtonPausePlay.setVisibility(View.GONE);
                    playerButtonPause.setVisibility(View.GONE);
                    playerWait.setVisibility(View.VISIBLE);
                    playerButtonRewind30.setVisibility(View.GONE);
                    break;
                case PLAYING:
                    playerButtonPausePlay.setVisibility(View.GONE);
                    playerButtonPause.setVisibility(View.VISIBLE);
                    playerWait.setVisibility(View.GONE);
                    playerButtonRewind30.setVisibility(View.VISIBLE);
                    break;
                case PAUSED:
                    playerButtonPausePlay.setVisibility(View.VISIBLE);
                    playerButtonPause.setVisibility(View.GONE);
                    playerWait.setVisibility(View.GONE);
                    playerButtonRewind30.setVisibility(View.VISIBLE);

                    break;
            }
        });

//        audioViewModel.isPlayingLiveData()
//                      .observe(this, isPLaying -> {
//                          findViewById(R.id.play_button).setVisibility(isPLaying ? View.GONE : View.VISIBLE);
//                          findViewById(R.id.pause_button).setVisibility(!isPLaying ? View.GONE : View.VISIBLE);
//                      });
//        Intent intent = new Intent(this, AudioPlayerService.class);
//        bindService(intent, connection, 0);

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.KEEPSCREEN, false)) {
            Timber.i("Bildschirm bleibt an!");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            Timber.i("Bildschirm bleibt nicht an!");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        audioManager.abandonAudioFocus(ttsViewModel.getAudioFocusChangeListener());
        super.onDestroy();
    }

    private void loadIndexFragment() {

        mUserTocFragment = (UserTocFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_INDEX);
        if (mUserTocFragment == null) {
            Timber.i("Did not find IndexFragment, create one ...");
            mUserTocFragment = ReaderBaseFragment.newInstance(UserTocFragment.class, bookId);
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.left_drawer, mUserTocFragment, TAG_FRAGMENT_INDEX);
            indexesFragmentTransaction.commit();
        }

        //mIndexFragment.init(retainDataFragment.getPaper());
    }

    private void loadPageIndexFragment() {

        mPageTocFragment = (PageTocFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PAGEINDEX);
        if (mPageTocFragment == null) {
            Timber.i("Did not find PageIndexFragment, create one ...");
            mPageTocFragment = ReaderBaseFragment.newInstance(PageTocFragment.class, bookId);
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.right_drawer, mPageTocFragment, TAG_FRAGMENT_PAGEINDEX);
            indexesFragmentTransaction.commit();
        }
        //mPageIndexFragment.init(getReaderDataFragment().getPaper());
    }

    public void loadContentFragment(String key) {

        ITocItem indexItem = readerViewModel.getPaper()
                                            .getPlist()
                                            .getIndexItem(key);
        if (indexItem != null) {
            switch (indexItem.getType()) {
                case CATEGORY:

                    break;
                case ARTICLE:
                case TOPLINK:
                    loadArticleFragment(indexItem, DIRECTIONS.NONE, null);
//                    closeDrawers();
                    break;
                case PAGE:
                    loadPagesFragment(indexItem);
//                    closeDrawers();
                    break;
            }
        }
    }

    private void loadArticleFragment(String key, DIRECTIONS direction, String position) {
        ITocItem indexItem = readerViewModel.getPaper()
                                            .getPlist()
                                            .getIndexItem(key);
        loadArticleFragment(indexItem, direction, position);
    }

    private void loadArticleFragment(ITocItem indexItem, DIRECTIONS direction, String position) {


//        if (TextUtils.isEmpty(position)) position = readerViewModel.getStoreRepository()
//                                                                   .getStore(readerViewModel.getPaper()
//                                                                                            .getBookId(),
//                                                                             Paper.STORE_KEY_POSITION_IN_ARTICLE + "_" + indexItem.getKey())
//                                                                   .getValue("0");

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        //AbstractContentFragment oldContentFragment = (AbstractContentFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);

        //if (oldContentFragment != null) fragmentTransaction.remove(oldContentFragment);

        if (indexItem.getType() == ITocItem.Type.TOPLINK) {
            mContentFragment = TopLinkFragment.newInstance(bookId, indexItem.getKey());
        } else {
            mContentFragment = ArticleFragment.newInstance(bookId, indexItem.getKey(), position);
        }


        // important: Animation before replace!
        switch (direction) {
            case TOP:
                fragmentTransaction.setCustomAnimations(R.anim.in_from_top, R.anim.out_to_bottom);
                //fragmentTransaction.setCustomAnimations(R.animator.top_in, R.animator.bottom_out);
                break;
            case BOTTOM:
                fragmentTransaction.setCustomAnimations(R.anim.in_from_bottom, R.anim.out_to_top);
                //fragmentTransaction.setCustomAnimations(R.animator.bottom_in, R.animator.top_out);
                break;
            case LEFT:
                fragmentTransaction.setCustomAnimations(R.anim.in_from_left, R.anim.out_to_right);
                //fragmentTransaction.setCustomAnimations(R.animator.left_in, R.animator.right_out);
                break;
            case RIGHT:
                fragmentTransaction.setCustomAnimations(R.anim.in_from_right, R.anim.out_to_left);
                //fragmentTransaction.setCustomAnimations(R.animator.right_in, R.animator.left_out);
                break;
            default:
                break;
        }

        fragmentTransaction.replace(R.id.content_frame, mContentFragment);

        fragmentTransaction.commit();

        //mContentFragment.init(retainDataFragment.getPaper(), indexItem.getPath(), position);
    }

    private void loadPagesFragment(ITocItem indexItem) {
        readerViewModel.setCurrentKey(indexItem.getKey());
        if (indexItem.getType() == ITocItem.Type.PAGE) {
            if (mContentFragment == null) {
                mContentFragment = (AbstractContentFragment) mFragmentManager.findFragmentById(R.id.content_frame);
            }
            if (!(mContentFragment instanceof PagesFragment)) {
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                mContentFragment = ReaderBaseFragment.newInstance(PagesFragment.class, bookId);
//                mContentFragment = PagesFragment.newInstance(/*indexItem.getKey()*/);
                fragmentTransaction.replace(R.id.content_frame, mContentFragment);
                fragmentTransaction.commit();
            } /*else {
                ((PagesFragment) mContentFragment).setPage(indexItem.getKey());
            }*/
        }
    }

    public void onShowHelp() {
        if (mContentFragment instanceof PagesFragment) {
            showHelpDialog(HelpDialog.HELP_PAGE);
        } else {
            showHelpDialog(HelpDialog.HELP_ARTICLE);
        }
    }

    public boolean onLoadPrevArticle(DIRECTIONS fromDirection, String position) {

        int prevPosition = readerViewModel.getPaper()
                                          .getArticleCollectionOrderPosition(readerViewModel.getCurrentKey()) - 1;

        if (readerViewModel.getUserTocLiveData()
                           .isFilterBookmarks()) {
            while (prevPosition >= 0) {
                ITocItem item = readerViewModel.getPaper()
                                               .getPlist()
                                               .getIndexItem(readerViewModel.getPaper()
                                                                            .getArticleCollectionOrderKey(prevPosition));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                prevPosition--;
            }
        }

        if (prevPosition >= 0) {
            loadArticleFragment(readerViewModel.getPaper()
                                               .getArticleCollectionOrderKey(prevPosition), fromDirection, position);
            return true;
        }
        return false;
    }

    public boolean onLoadNextArticle(DIRECTIONS fromDirection, String position) {

        int nextPosition = readerViewModel.getPaper()
                                          .getArticleCollectionOrderPosition(readerViewModel.getCurrentKey()) + 1;

        if (readerViewModel.getUserTocLiveData()
                           .isFilterBookmarks()) {
            while (nextPosition < readerViewModel.getPaper()
                                                 .getArticleCollectionSize()) {
                ITocItem item = readerViewModel.getPaper()
                                               .getPlist()
                                               .getIndexItem(readerViewModel.getPaper()
                                                                            .getArticleCollectionOrderKey(nextPosition));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                nextPosition++;
            }
        }

        if (nextPosition < readerViewModel.getPaper()
                                          .getArticleCollectionSize()) {

            loadArticleFragment(readerViewModel.getPaper()
                                               .getArticleCollectionOrderKey(nextPosition), fromDirection, position);
            return true;
        }
        return false;
    }


    public void setBackgroundColor(int color) {
        Timber.d("%d", color);
        this.findViewById(android.R.id.content)
            .setBackgroundColor(color);
    }

    public int onGetBackgroundColor(String themeName) {
        THEMES theme = THEMES.valueOf(themeName);
        // THEMES theme = THEMES.valueOf(TazSettings.getPrefString(this, TazSettings.PREFKEY.THEME, "normal"));
        String hexColor = TazSettings.getInstance(this)
                                     .getPrefString(theme.getBgColorName(), "#FFFFFF");
        return Color.parseColor(hexColor);
    }

//    @Override
//    public boolean onLoad(String key) {
//
//        loadContentFragment(key);
//        return false;
//    }


    public void onBookmarkClick(ITocItem item) {
        Timber.d("%s", item.getKey());
        item.setBookmark(!item.isBookmarked());

        readerViewModel.getUserTocLiveData()
                       .onBookmarkChanged(item);

//        if (mUserTocFragment != null) mUserTocFragment.onBookmarkChange(item.getKey());
        ITocItem currentItem = readerViewModel.getCurrentKeyLiveData()
                                              .getValue();
        if (currentItem.equals(item)) {
            if (mContentFragment instanceof ArticleFragment) ((ArticleFragment) mContentFragment).initialBookmark();
        }
        new AsyncTaskListener<JSONArray, Void>(new AsyncTaskListener.OnExecute<JSONArray, Void>() {
            @Override
            public Void execute(JSONArray... jsonArrays) {
                JSONArray jsonArray = jsonArrays[0];

                Store bookmarkStore = readerViewModel.getStoreRepository()
                                                     .getStore(readerViewModel.getPaper()
                                                                              .getBookId(), Paper.STORE_KEY_BOOKMARKS);

                if (jsonArray.length() > 0) {
                    bookmarkStore.setValue(jsonArray.toString());
                    readerViewModel.getStoreRepository()
                                   .saveStore(bookmarkStore);
                } else {
                    readerViewModel.getStoreRepository()
                                   .deleteStore(bookmarkStore);
                }

                return null;
            }
        }).execute(readerViewModel.getPaper()
                                  .getBookmarkJson());

    }

    @Override
    public void onDialogClick(String tag, Bundle arguments, int which) {
        super.onDialogClick(tag, arguments, which);
        if (TAG_DIALOG_TTS_ERROR.equals(tag)) {
            if (which == Dialog.BUTTON_NEUTRAL) {
                Intent intent = new Intent();
                intent.setAction("com.android.settings.TTS_SETTINGS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
            }
        }
    }

    @Override
    public void onDialogDismiss(String tag, Bundle arguments) {
        super.onDialogDismiss(tag, arguments);
        if (TAG_FRAGMENT_DIALOG_SETTING.equals(tag)) {
            //setImmersiveMode();
        }
    }

    public void closeDrawers() {
        mDrawerLayout.postDelayed(() -> mDrawerLayout.closeDrawers(), 500);
    }

    public void openPageIndexDrawer() {
        mDrawerLayout.openDrawer(mDrawerLayoutPageIndex);
    }

    public void openIndexDrawer() {
        mDrawerLayout.openDrawer(mDrawerLayoutIndex);
    }


    public void togglePageIndexDrawer() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLayoutPageIndex)) {
            mDrawerLayout.closeDrawer(mDrawerLayoutPageIndex);
        } else {
            mDrawerLayout.openDrawer(mDrawerLayoutPageIndex);
        }

    }

    @Override
    public void onConfigurationChange(String name, String value) {
        Timber.d("%s %s", name, value);
        if (TazSettings.PREFKEY.THEME.equals(name)) setBackgroundColor(onGetBackgroundColor(value));
        callConfigListeners(name, value);
    }

    @Override
    public void onConfigurationChange(String name, boolean value) {
        Timber.d("%s %s", name, value);
        String boolValue = "off";
        if (value) boolValue = "on";
        callConfigListeners(name, boolValue);
    }

    WeakHashMap<ConfigurationChangeListener, Void> configListenerWeakHashMap = new WeakHashMap<>();

    public interface ConfigurationChangeListener {

        void onConfigurationChange(String key, String value);
    }


    public void addConfigChangeListener(ConfigurationChangeListener listener) {
        configListenerWeakHashMap.put(listener, null);
    }

    private void callConfigListeners(String key, String value) {
        for (ConfigurationChangeListener listener : configListenerWeakHashMap.keySet()) {
            listener.onConfigurationChange(key, value);
        }
    }

    @Override
    public void onBackPressed() {
        if (readerViewModel.getCurrentKeyLiveData()
                           .getValue() != null) {
            ITocItem currentItem = readerViewModel.getPaper()
                                                  .getPlist()
                                                  .getIndexItem(readerViewModel.getCurrentKey());
            if (currentItem instanceof Paper.Plist.Page.Article) {
                loadContentFragment(((Paper.Plist.Page.Article) currentItem).getRealPage()
                                                                            .getKey());
                return;
            }
        }
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                togglePageIndexDrawer();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void setImmersiveMode() {

        boolean onOff = TazSettings.getInstance(this)
                                   .getPrefBoolean(TazSettings.PREFKEY.FULLSCREEN, false);

        mContentFrame.setFitsSystemWindows(!onOff);

        int newUiOptions = getWindow().getDecorView()
                                      .getSystemUiVisibility();

        if (onOff) {
            // Navigation bar hiding:  Backwards compatible to ICS.
            if (Build.VERSION.SDK_INT >= 14) {
                newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // Status bar hiding: Backwards compatible to Jellybean
            if (Build.VERSION.SDK_INT >= 16) {
                //newUiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
                newUiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                newUiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            }
            // Immersive mode: Backward compatible to KitKat.
            // Note that this flag doesn't do anything by itself, it only augments the behavior
            // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
            // all three flags are being toggled together.
            // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
            // Sticky immersive mode differs in that it makes the navigation and status bars
            // semi-transparent, and the UI flag does not get cleared when the user interacts with
            // the screen.
            if (Build.VERSION.SDK_INT >= 19) {
                newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        } else {
            newUiOptions = 0;
        }


        getWindow().getDecorView()
                   .setSystemUiVisibility(newUiOptions);
        mContentFrame.requestLayout();

    }

//    @Override
//    public void onTtsStateChanged(ReaderTtsFragment.TTS newState) {
//        Timber.d(newState.name());
//        if (mContentFragment != null) mContentFragment.onTtsStateChanged(newState);
//    }
//
//    @Override
//    public void onTtsInitError(ReaderTtsFragment.TTSERROR error) {
//        Timber.w("error: %s", error);
//        StringBuilder message = new StringBuilder(getString(R.string.dialog_tts_error));
//        switch (error) {
//            case LANG_MISSING_DATA:
//                message.append(" ")
//                       .append(getString(R.string.dialog_tts_error_lang_missing_data));
//                break;
//            case LANG_NOT_SUPPORTED:
//                message.append(" ")
//                       .append(getString(R.string.dialog_tts_error_lang_not_supported));
//                break;
//        }
//        new Dialog.Builder().setMessage(message.toString())
//                            .setNeutralButton(R.string.dialog_tts_error_settings)
//                            .setPositiveButton()
//                            .buildSupport()
//                            .show(getSupportFragmentManager(), TAG_DIALOG_TTS_ERROR);
//    }
//
//    @Override
//    public void onTtsStopped() {
//        audioManager.abandonAudioFocus(retainTtsFragment.getAudioFocusChangeListener());
//        if (retainTtsFragment.getTtsState() == ReaderTtsFragment.TTS.PAUSED) {
//            showTtsSnackbar(getString(R.string.toast_tts_paused),
//                            getString(R.string.toast_tts_action_restart),
//                            new View.OnClickListener() {
//                                @Override
//                                public void onClick(View v) {
//                                    retainTtsFragment.stopTts();
//                                    if (ttsPreparePlayingInActivity()) {
//                                        retainTtsFragment.restartTts();
//                                    }
//                                }
//                            });
//        }
//
//    }


    public boolean ttsPreparePlayingInActivity() {

        int request = audioManager.requestAudioFocus(ttsViewModel.getAudioFocusChangeListener(),
                                                     TextToSpeech.Engine.DEFAULT_STREAM,
                                                     AudioManager.AUDIOFOCUS_GAIN);
        switch (request) {
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:

                switch (ttsViewModel.getTtsState()) {
                    case DISABLED:
                    case IDLE:
                        showTtsSnackbar(makeTtsPlayingSpan(getString(R.string.toast_tts_started)));
                        break;
                    case PAUSED:
                        showTtsSnackbar(makeTtsPlayingSpan(getString(R.string.toast_tts_continued)),
                                        getString(R.string.toast_tts_action_restart),
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                ttsViewModel.stopTts();
                                                ttsViewModel.restartTts();
                                            }
                                        });
                }
                return true;


            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                break;

        }
        return false;
    }

    @SuppressLint("StringFormatInvalid")
    private CharSequence makeTtsPlayingSpan(String text) {
        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        snackbarText.append(text);
        int volume = audioManager.getStreamVolume(TextToSpeech.Engine.DEFAULT_STREAM);
        int maxVolume = audioManager.getStreamMaxVolume(TextToSpeech.Engine.DEFAULT_STREAM);
        int percent = volume * 100 / maxVolume;
        if (percent <= 20) {
            int boldStart = snackbarText.length();
            snackbarText.append(getString(R.string.toast_tts_volume_warning, percent));
            snackbarText.setSpan(new ForegroundColorSpan(Color.RED),
                                 boldStart,
                                 snackbarText.length(),
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            snackbarText.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                                 boldStart,
                                 snackbarText.length(),
                                 Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return snackbarText;
    }


//    public ReaderTtsFragment.TTS getTtsState() {
//        Timber.d("%s", retainTtsFragment.getTtsState());
//        return retainTtsFragment.getTtsState();
//    }

    public void speak2(Paper.Plist.Page.Article article) {



        if (article != null && article.getAudiolink() != null) {
            Uri audioUri;
            if (URLUtil.isNetworkUrl(article.getAudiolink())) {
                audioUri = Uri.parse(article.getAudiolink());
            } else {
                audioUri = Uri.fromFile(new File(readerViewModel.getPaperDirectory(), article.getAudiolink()));
            }
            if (audioUri != null) {

                AudioItem audioItem = new AudioItem(audioUri.toString(),
                                                    article.getTitle(),
                                                    article.getPaper()
                                                           .getTitelWithDate(this),
                                                    article.getPaper().getBookId(),
                                                    0,0);

                audioViewModel.startPlaying(audioItem);

            }
        }
    }

    public void speak(@NonNull String id, CharSequence text) {
        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.TEXTTOSPEACH, false)) {
            switch (ttsViewModel.getTtsState()) {
                case DISABLED:
                    if (ttsPreparePlayingInActivity()) {
                        ttsViewModel.initTts(id, text);
                    }
                    break;
                case PLAYING:
                    ttsViewModel.pauseTts();
                    break;
                case IDLE:
                    if (ttsPreparePlayingInActivity()) {
                        ttsViewModel.flushTts();
                        ttsViewModel.prepareTts(id, text);
                        ttsViewModel.startTts();
                    }
                    break;
                case PAUSED:
                    if (ttsPreparePlayingInActivity()) {
                        if (id.equals(ttsViewModel.getUtteranceBaseId())) {
                            ttsViewModel.startTts();
                        } else {
                            ttsViewModel.flushTts();
                            speak(id, text);
                        }
                    }
                    break;
            }
        }
    }

    public void showTtsSnackbar(CharSequence message) {
        showTtsSnackbar(message, null, null);
    }

    public void showTtsSnackbar(CharSequence message, String action, View.OnClickListener actionListener) {
        Snacky.builder()
              .setActivity(this)
              .setText(message)
              .setIcon(TintHelper.tintAndReturnDrawable(ContextCompat.getDrawable(this,
                                                                                  R.drawable.ic_record_voice_over_black_24dp),
                                                        Color.WHITE))
              .setDuration(Snacky.LENGTH_LONG)
              .setActionText(action)
              .setActionClickListener(actionListener)
              .build()
              .show();
    }
}
