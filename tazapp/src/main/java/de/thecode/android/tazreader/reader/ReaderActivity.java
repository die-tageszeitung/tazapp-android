package de.thecode.android.tazreader.reader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.listener.DialogButtonListener;
import de.mateware.dialog.listener.DialogDismissListener;
import de.mateware.snacky.Snacky;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.analytics.AnalyticsWrapper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.HelpDialog;
import de.thecode.android.tazreader.download.NotificationHelper;
import de.thecode.android.tazreader.reader.article.ArticleFragment;
import de.thecode.android.tazreader.reader.article.TopLinkFragment;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.reader.index.IndexFragment;
import de.thecode.android.tazreader.reader.index.PageIndexFragment;
import de.thecode.android.tazreader.reader.page.PagesFragment;
import de.thecode.android.tazreader.utils.BaseActivity;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.TintHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;

import java.util.WeakHashMap;

import timber.log.Timber;

@SuppressLint("RtlHardcoded")
public class ReaderActivity extends BaseActivity
        implements IReaderCallback, DialogButtonListener, DialogDismissListener, ReaderTtsFragment.ReaderTtsFragmentCallback {

    private AudioManager audioManager;

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

    private static final String TAG_FRAGMENT_INDEX            = "IndexFragment";
    private static final String TAG_FRAGMENT_PAGEINDEX        = "PageIndexFragment";
    public static final  String TAG_FRAGMENT_DIALOG_SETTING   = "settingsDialog";
    public static final  String TAG_DIALOG_TTS_ERROR          = "ttsError";
    public static final  String KEY_EXTRA_PAPER_ID            = "paperId";
    public static final  String STORE_KEY_BOOKMARKS           = "bookmarks";
    public static final  String STORE_KEY_CURRENTPOSITION     = "currentPosition";
    public static final  String STORE_KEY_POSITION_IN_ARTICLE = "positionInArticle";

    DrawerLayout mDrawerLayout;
    View         mDrawerLayoutIndex;
    View         mDrawerLayoutPageIndex;
    FrameLayout  mContentFrame;
    ProgressBar  mLoadingProgress;

    FragmentManager mFragmentManager;
    StorageManager  mStorage;

    //ReaderDataFragment retainDataFragment;
    ReaderDataFragment retainDataFragmentNew;
    ReaderTtsFragment  retainTtsFragment;

    IndexFragment           mIndexFragment;
    PageIndexFragment       mPageIndexFragment;
    AbstractContentFragment mContentFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Orientation.setActivityOrientationFromPrefs(this);

        mStorage = StorageManager.getInstance(this);

        long paperId;
        if (!getIntent().hasExtra(KEY_EXTRA_PAPER_ID))
            throw new IllegalStateException("Activity Reader has to be called with extra PaperId");
        else paperId = getIntent().getLongExtra(KEY_EXTRA_PAPER_ID, -1);
        if (paperId == -1) throw new IllegalStateException("paperId must not be " + paperId);

        NotificationHelper.removeNotifiedPaperId(this, paperId);

        setContentView(R.layout.activity_reader);


        setBackgroundColor(onGetBackgroundColor(TazSettings.getInstance(this)
                                                           .getPrefString(TazSettings.PREFKEY.THEME, "normal")));

        mLoadingProgress = (ProgressBar) findViewById(R.id.loading);
        mLoadingProgress.setVisibility(View.VISIBLE);

        mDrawerLayoutPageIndex = findViewById(R.id.right_drawer);
        DrawerLayout.LayoutParams pageIndexLayoutParams = (DrawerLayout.LayoutParams) mDrawerLayoutPageIndex.getLayoutParams();
        pageIndexLayoutParams.width = getResources().getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) + (getResources().getDimensionPixelSize(
                R.dimen.pageindex_padding) * 2);
        mDrawerLayoutPageIndex.setLayoutParams(pageIndexLayoutParams);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayoutIndex = findViewById(R.id.left_drawer);
        mContentFrame = (FrameLayout) findViewById(R.id.content_frame);

        mFragmentManager = getSupportFragmentManager();

        if (getReaderDataFragment().isPaperLoaded()) {
            onPaperLoadFinished(new PaperLoadedEvent());
        } else {
            getReaderDataFragment().loadPaper(paperId);
        }

//        retainDataFragment = ReaderDataFragment.retainDataFragment(getSupportFragmentManager(), ReaderDataFragment.class);
//        if (retainDataFragment != null) {
//            Timber.i("Found data fragment");
//            if (retainDataFragment.isPaperLoaded()) {
//                onPaperLoadFinished(new PaperLoadedEvent());
//            }
//        } else {
//            Timber.i("Did not find data fragment, initialising loading");
//            retainDataFragment = ReaderDataFragment.createDataFragment(getSupportFragmentManager(), ReaderDataFragment.class);
//            retainDataFragment.loadPaper(paperId);
//        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        retainTtsFragment = ReaderTtsFragment.createOrRetainDataFragment(getSupportFragmentManager(), ReaderTtsFragment.class);
        retainTtsFragment.setCallback(this);
        if (retainTtsFragment.getTtsState() == ReaderTtsFragment.TTS.PLAYING) ttsPreparePlayingInActivty();
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
        audioManager.abandonAudioFocus(retainTtsFragment.getAudioFocusChangeListener());
        super.onDestroy();
    }

    private void initializeFragments() {
        loadIndexFragment();
        loadPageIndexFragment();
        loadContentFragment(getReaderDataFragment().getCurrentKey());
    }

    public ReaderDataFragment getReaderDataFragment() {
        if (retainDataFragmentNew == null) retainDataFragmentNew = ReaderDataFragment.createOrRetainDataFragment(
                getSupportFragmentManager(),
                ReaderDataFragment.class);
        return retainDataFragmentNew;
    }

    private void loadIndexFragment() {

        mIndexFragment = (IndexFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_INDEX);
        if (mIndexFragment == null) {
            Timber.i("Did not find IndexFragment, create one ...");
            mIndexFragment = new IndexFragment();
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.left_drawer, mIndexFragment, TAG_FRAGMENT_INDEX);
            indexesFragmentTransaction.commit();
        }

        //mIndexFragment.init(retainDataFragment.getPaper());
    }

    private void loadPageIndexFragment() {

        mPageIndexFragment = (PageIndexFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PAGEINDEX);
        if (mPageIndexFragment == null) {
            Timber.i("Did not find PageIndexFragment, create one ...");
            mPageIndexFragment = new PageIndexFragment();
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.right_drawer, mPageIndexFragment, TAG_FRAGMENT_PAGEINDEX);
            indexesFragmentTransaction.commit();
        }
        //mPageIndexFragment.init(getReaderDataFragment().getPaper());
    }

    private void loadContentFragment(String key) {

        IIndexItem indexItem = getReaderDataFragment().getPaper()
                                                      .getPlist()
                                                      .getIndexItem(key);
        if (indexItem != null) {
            switch (indexItem.getType()) {
                case ARTICLE:
                case TOPLINK:
                    loadArticleFragment(indexItem, DIRECTIONS.NONE, null);
                    break;
                case PAGE:
                    loadPagesFragment(indexItem);
                    break;
            }
        }
    }

    private void loadArticleFragment(String key, DIRECTIONS direction, String position) {
        IIndexItem indexItem = getReaderDataFragment().getPaper()
                                                      .getPlist()
                                                      .getIndexItem(key);
        loadArticleFragment(indexItem, direction, position);
    }

    private void loadArticleFragment(IIndexItem indexItem, DIRECTIONS direction, String position) {


        if (TextUtils.isEmpty(position)) position = getPaper().getPositionInArticle(this,indexItem);

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        if (indexItem.getType() == IIndexItem.Type.TOPLINK) {
            mContentFragment = TopLinkFragment.newInstance(indexItem.getKey());
        } else {
            mContentFragment = ArticleFragment.newInstance(indexItem.getKey(),position);
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

        //mContentFragment.init(retainDataFragment.getPaper(), indexItem.getKey(), position);
    }

    private void loadPagesFragment(IIndexItem indexItem) {




//        AnalyticsWrapper.getInstance()
//                        .trackBreadcrumb("loadPagesFragment...");
        if (indexItem.getType() == IIndexItem.Type.PAGE) {
//            boolean needInit = false;
            if (mContentFragment == null) {
                mContentFragment = (AbstractContentFragment) mFragmentManager.findFragmentById(R.id.content_frame);
//                needInit = true;
            }
            if (!(mContentFragment instanceof PagesFragment)) {
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                mContentFragment = PagesFragment.newInstance(indexItem.getKey());
                fragmentTransaction.replace(R.id.content_frame, mContentFragment);
                fragmentTransaction.commit();
//                needInit = true;
            } else {
                ((PagesFragment) mContentFragment).setPage(indexItem.getKey());
            }
//            if (needInit) {
//                AnalyticsWrapper.getInstance()
//                                .trackBreadcrumb("...with init of Fragment");
//                mContentFragment.init(retainDataFragment.getPaper(), indexItem.getKey(), "");
//            } else ((PagesFragment) mContentFragment).setPage(indexItem.getKey());
        }
    }

    @Override
    public void onShowHelp() {
        if (mContentFragment instanceof PagesFragment) {
            showHelpDialog(HelpDialog.HELP_PAGE);
        } else {
            showHelpDialog(HelpDialog.HELP_ARTICLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPaperLoadFinished(PaperLoadedEvent event) {
        mLoadingProgress.setVisibility(View.GONE);
        if (!event.hasError()) {
            initializeFragments();
        } else {
            Timber.e(event.getException());
//                    AnalyticsWrapper.getInstance()
//                                    .logException(result.getError());
            ReaderActivity.this.finish();
        }
    }
//
//    @Override
//    public Loader<PaperLoader.PaperLoaderResult> onCreateLoader(int arg0, Bundle arg1) {
//
//        mLoadingProgress.setVisibility(View.VISIBLE);
//        return new PaperLoader(this, paperId);
//    }
//
//    @Override
//    public void onLoadFinished(Loader<PaperLoader.PaperLoaderResult> loader, final PaperLoader.PaperLoaderResult result) {
//        EventBus.getDefault()
//                .post(new PaperLoadedEvent(result));
//    }
//
//    @Override
//    public void onLoaderReset(Loader<PaperLoader.PaperLoaderResult> arg0) {
//
//    }

    @Override
    public boolean onLoadPrevArticle(DIRECTIONS fromDirection, String position) {

        int prevPosition = getReaderDataFragment().getPaper()
                                                  .getArticleCollectionOrderPosition(getReaderDataFragment().getCurrentKey()) - 1;

        if (getReaderDataFragment().isFilterBookmarks()) {
            while (prevPosition >= 0) {
                IIndexItem item = getReaderDataFragment().getPaper()
                                                         .getPlist()
                                                         .getIndexItem(getReaderDataFragment().getPaper()
                                                                                              .getArticleCollectionOrderKey(
                                                                                                      prevPosition));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                prevPosition--;
            }
        }

        if (prevPosition >= 0) {
            loadArticleFragment(getReaderDataFragment().getPaper()
                                                       .getArticleCollectionOrderKey(prevPosition), fromDirection, position);
            return true;
        }
        return false;
    }

    @Override
    public boolean onLoadNextArticle(DIRECTIONS fromDirection, String position) {

        int nextPositiion = getReaderDataFragment().getPaper()
                                                   .getArticleCollectionOrderPosition(getReaderDataFragment().getCurrentKey()) + 1;

        if (getReaderDataFragment().isFilterBookmarks()) {
            while (nextPositiion < getReaderDataFragment().getPaper()
                                                          .getArticleCollectionSize()) {
                IIndexItem item = getReaderDataFragment().getPaper()
                                                         .getPlist()
                                                         .getIndexItem(getReaderDataFragment().getPaper()
                                                                                              .getArticleCollectionOrderKey(
                                                                                                      nextPositiion));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                nextPositiion++;
            }
        }

        if (nextPositiion < getReaderDataFragment().getPaper()
                                                   .getArticleCollectionSize()) {

            loadArticleFragment(getReaderDataFragment().getPaper()
                                                       .getArticleCollectionOrderKey(nextPositiion), fromDirection, position);
            return true;
        }
        return false;
    }


    public void setBackgroundColor(int color) {
        Timber.d("%d", color);
        this.findViewById(android.R.id.content)
            .setBackgroundColor(color);
    }

    @Override
    public int onGetBackgroundColor(String themeName) {
        THEMES theme = THEMES.valueOf(themeName);
        // THEMES theme = THEMES.valueOf(TazSettings.getPrefString(this, TazSettings.PREFKEY.THEME, "normal"));
        String hexColor = TazSettings.getInstance(this)
                                     .getPrefString(theme.getBgColorName(), "#FFFFFF");
        return Color.parseColor(hexColor);
    }

    @Override
    public boolean onLoad(String key) {
        loadContentFragment(key);
        return false;
    }


    @Override
    public void onBookmarkClick(IIndexItem item) {
        Timber.d("%s", item.getKey());
        item.setBookmark(!item.isBookmarked());
        if (mIndexFragment != null) mIndexFragment.onBookmarkChange(item.getKey());
        if (item.getKey()
                .equals(getReaderDataFragment().getCurrentKey())) {
            if (mContentFragment instanceof ArticleFragment) ((ArticleFragment) mContentFragment).initialBookmark();
        }
        JSONArray bookmarks = getPaper().getBookmarkJson();
        if (bookmarks.length() > 0) {
            getPaper().saveStoreValue(this, STORE_KEY_BOOKMARKS, bookmarks.toString());
        } else {
            getPaper().deleteStoreKey(this, STORE_KEY_BOOKMARKS);
        }

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

    @Override
    public void closeDrawers() {
        mDrawerLayout.closeDrawer(mDrawerLayoutIndex);
        mDrawerLayout.closeDrawer(mDrawerLayoutPageIndex);
        //mDrawerLayout.closeDrawers();
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

        public void onConfigurationChange(String key, String value);
    }

    @Override
    public void addConfigChangeListener(ConfigurationChangeListener listener) {
        configListenerWeakHashMap.put(listener, null);
    }

    private void callConfigListeners(String key, String value) {
        for (ConfigurationChangeListener listener : configListenerWeakHashMap.keySet()) {
            listener.onConfigurationChange(key, value);
        }
    }

    @Override
    public void updateIndexes(String key) {
        getReaderDataFragment().setCurrentKey(key);
        if (mIndexFragment != null) mIndexFragment.updateCurrentPosition(key);
        if (mPageIndexFragment != null) mPageIndexFragment.updateCurrentPosition(key);
        if (mContentFragment instanceof PagesFragment)
            ((PagesFragment) mContentFragment).setShareButtonCallback(getReaderDataFragment().getPaper()
                                                                                             .getPlist()
                                                                                             .getIndexItem(key));
    }


    @Override
    public String getCurrentKey() {
        return getReaderDataFragment().getCurrentKey();
    }

    @Override
    public String getStoreValue(String path, String value) {
        String result = Store.getValueForKey(this, "/" + getPaper().getBookId() + "/" + path);
        return result;
    }

    @Override
    public void setFilterBookmarks(boolean bool) {
        getReaderDataFragment().setFilterBookmarks(bool);
    }

    @Override
    public boolean isFilterBookmarks() {
        return getReaderDataFragment().isFilterBookmarks();
    }


    @Override
    public Paper getPaper() {
        Timber.i("");
        return getReaderDataFragment().getPaper();
    }


    @Override
    public void onBackPressed() {
        if (getReaderDataFragment() != null && getReaderDataFragment().getCurrentKey() != null) {
            IIndexItem currentItem = getReaderDataFragment().getPaper()
                                                            .getPlist()
                                                            .getIndexItem(getReaderDataFragment().getCurrentKey());
            if (currentItem instanceof Paper.Plist.Page.Article) {
                onLoad(((Paper.Plist.Page.Article) currentItem).getRealPage()
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

    @Override
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
            if (Build.VERSION.SDK_INT >= 18) {
                newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
        } else {
            newUiOptions = 0;
        }


        getWindow().getDecorView()
                   .setSystemUiVisibility(newUiOptions);
        mContentFrame.requestLayout();

    }

    @Override
    public void onTtsStateChanged(ReaderTtsFragment.TTS newState) {
        Timber.d(newState.name());
        if (mContentFragment != null) mContentFragment.onTtsStateChanged(newState);
    }

    @Override
    public void onTtsInitError(ReaderTtsFragment.TTSERROR error) {
        Timber.w("error: %s", error);
        StringBuilder message = new StringBuilder(getString(R.string.dialog_tts_error));
        switch (error) {
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


    public boolean ttsPreparePlayingInActivty() {

        int request = audioManager.requestAudioFocus(retainTtsFragment.getAudioFocusChangeListener(),
                                                     TextToSpeech.Engine.DEFAULT_STREAM,
                                                     AudioManager.AUDIOFOCUS_GAIN);
        switch (request) {
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:

                switch (retainTtsFragment.getTtsState()) {
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
                                                retainTtsFragment.stopTts();
                                                retainTtsFragment.restartTts();
                                            }
                                        });
                }
                return true;


            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                break;

        }
        return false;
    }

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


    @Override
    public void onTtsStopped() {
        audioManager.abandonAudioFocus(retainTtsFragment.getAudioFocusChangeListener());
        if (retainTtsFragment.getTtsState() == ReaderTtsFragment.TTS.PAUSED) {
            showTtsSnackbar(getString(R.string.toast_tts_paused),
                            getString(R.string.toast_tts_action_restart),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    retainTtsFragment.stopTts();
                                    if (ttsPreparePlayingInActivty()) {
                                        retainTtsFragment.restartTts();
                                    }
                                }
                            });
        }

    }

    @Override
    public ReaderTtsFragment.TTS getTtsState() {
        Timber.d("%s", retainTtsFragment.getTtsState());
        return retainTtsFragment.getTtsState();
    }

    @Override
    public void speak(@NonNull String id, CharSequence text) {
        if (TazSettings.getInstance(this)
                       .getPrefBoolean(TazSettings.PREFKEY.TEXTTOSPEACH, false)) {
            switch (getTtsState()) {
                case DISABLED:
                    if (ttsPreparePlayingInActivty()) {
                        retainTtsFragment.initTts(this, id, text);
                    }
                    break;
                case PLAYING:
                    retainTtsFragment.pauseTts();
                    break;
                case IDLE:
                    if (ttsPreparePlayingInActivty()) {
                        retainTtsFragment.flushTts();
                        retainTtsFragment.prepareTts(id, text);
                        retainTtsFragment.startTts();
                    }
                    break;
                case PAUSED:
                    if (ttsPreparePlayingInActivty()) {
                        if (id.equals(retainTtsFragment.getUtteranceBaseId())) {
                            retainTtsFragment.startTts();
                        } else {
                            retainTtsFragment.flushTts();
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
              .setActivty(this)
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
