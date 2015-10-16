package de.thecode.android.tazreader.reader;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.common.base.Strings;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Book;
import de.thecode.android.tazreader.data.Paper.Plist.Category;
import de.thecode.android.tazreader.data.Paper.Plist.Page;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.data.Paper.Plist.Source;
import de.thecode.android.tazreader.data.Paper.Plist.TopLink;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.dialog.TcDialog;
import de.thecode.android.tazreader.dialog.TcDialog.TcDialogButtonListener;
import de.thecode.android.tazreader.download.NotificationHelper;
import de.thecode.android.tazreader.reader.article.ArticleFragment;
import de.thecode.android.tazreader.reader.article.TopLinkFragment;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.reader.index.IndexFragment;
import de.thecode.android.tazreader.reader.index.PageIndexFragment;
import de.thecode.android.tazreader.reader.page.PagesFragment;
import de.thecode.android.tazreader.utils.BaseActivity;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.Log;
import de.thecode.android.tazreader.utils.Utils;

@SuppressLint("RtlHardcoded")
public class ReaderActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<PaperLoader.PaperLoaderResult>, IReaderCallback, TcDialogButtonListener, TcDialog.TcDialogDismissListener {

    public static enum THEMES {
        normal("bgColorNormal"), sepia("bgColorSepia"), night("bgColorNight");

        private String bgColorName;

        private THEMES(String bgColorName) {
            this.bgColorName = bgColorName;
        }

        public String getBgColorName() {
            return bgColorName;
        }
    }

    public static enum DIRECTIONS {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    private static final String TAG_FRAGMENT_INDEX = "IndexFragment";
    private static final String TAG_FRAGMENT_PAGEINDEX = "PageIndexFragment";

    public static final String TAG_FRAGMENT_DIALOG_SETTING = "settingsDialog";

    public static final String KEY_EXTRA_PAPER_ID = "paperId";

    public static final String STORE_KEY_BOOKMARKS = "bookmarks";
    public static final String STORE_KEY_CURRENTPOSITION = "currentPosition";
    public static final String STORE_KEY_POSITION_IN_ARTICLE = "positionInArticle";

    private static final int LOADER_ID_PAPER = 2;

    long paperId;

    DrawerLayout mDrawerLayout;
    View mDrawerLayoutIndex;
    View mDrawerLayoutPageIndex;
    FrameLayout mContentFrame;

    FragmentManager mFragmentManager;

    StorageManager mStorage;

    RetainDataFragment retainDataFragment;

    IndexFragment mIndexFragment;

    PageIndexFragment mPageIndexFragment;

    AbstractContentFragment mContentFragment;

    ProgressBar mLoadingProgress;

    //Handler mUiThreadHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v();
        Utils.setActivityOrientationFromPrefs(this);

        mStorage = StorageManager.getInstance(this);

        //mUiThreadHandler = new Handler(Looper.getMainLooper());

        if (!getIntent().hasExtra(KEY_EXTRA_PAPER_ID)) throw new IllegalStateException("Activity Reader has to be called with extra PaperId");
        else paperId = getIntent().getLongExtra(KEY_EXTRA_PAPER_ID, -1);
        if (paperId == -1) throw new IllegalStateException("paperId must not be " + paperId);

        NotificationHelper.removeNotifiedPaperId(this, paperId);

        setContentView(R.layout.activity_reader);


        setBackgroundColor(onGetBackgroundColor(TazSettings.getPrefString(this, TazSettings.PREFKEY.THEME, "normal")));

        mLoadingProgress = (ProgressBar) findViewById(R.id.loading);
        mLoadingProgress.setVisibility(View.GONE);

        mDrawerLayoutPageIndex = findViewById(R.id.right_drawer);
        DrawerLayout.LayoutParams pageIndexLayoutParams = (DrawerLayout.LayoutParams) mDrawerLayoutPageIndex.getLayoutParams();
        pageIndexLayoutParams.width = getResources().getDimensionPixelSize(R.dimen.pageindex_thumbnail_image_width) + (getResources().getDimensionPixelSize(R.dimen.pageindex_padding) * 2);
        mDrawerLayoutPageIndex.setLayoutParams(pageIndexLayoutParams);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayoutIndex = findViewById(R.id.left_drawer);
        mContentFrame = (FrameLayout) findViewById(R.id.content_frame);


        mFragmentManager = getFragmentManager();


        retainDataFragment = RetainDataFragment.findRetainFragment(getFragmentManager());
        if (retainDataFragment != null && retainDataFragment.getPaper() != null) {
            Log.d("Found data fragment");
            initializeFragments();
        } else {
            retainDataFragment = RetainDataFragment.createRetainFragment(getFragmentManager());
            Log.d("Did not find data fragment, initialising loader");
            LoaderManager lm = getLoaderManager();
            Bundle paperLoaderBundle = new Bundle();
            paperLoaderBundle.putLong(KEY_EXTRA_PAPER_ID, paperId);
            lm.initLoader(LOADER_ID_PAPER, paperLoaderBundle, this);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        Log.v(fragment.getTag());
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.v();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v();
        if (TazSettings.getPrefBoolean(this, TazSettings.PREFKEY.KEEPSCREEN, false)) {
            Log.d("Bildschirm bleibt an!");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            Log.d("Bildschirm bleibt nicht an!");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        //setImmersiveMode();
    }

    @Override
    protected void onPause() {
        Log.v();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        Log.v();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v();
        super.onDestroy();
    }

    private void initializeFragments() {
        loadIndexFragment();
        loadPageIndexFragment();
        loadContentFragment(retainDataFragment.getCurrentKey(), retainDataFragment.getPostion());
    }

    private void loadIndexFragment() {
        Log.v();
        mIndexFragment = (IndexFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_INDEX);
        if (mIndexFragment == null) {
            Log.d("Did not find IndexFragment, create one ...");
            mIndexFragment = new IndexFragment();
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.left_drawer, mIndexFragment, TAG_FRAGMENT_INDEX);
            indexesFragmentTransaction.commitAllowingStateLoss();
        }

        mIndexFragment.init(retainDataFragment.getPaper());
    }

    private void loadPageIndexFragment() {
        Log.v();
        mPageIndexFragment = (PageIndexFragment) mFragmentManager.findFragmentByTag(TAG_FRAGMENT_PAGEINDEX);
        if (mPageIndexFragment == null) {
            Log.d("Did not find PageIndexFragment, create one ...");
            mPageIndexFragment = new PageIndexFragment();
            FragmentTransaction indexesFragmentTransaction = mFragmentManager.beginTransaction();
            indexesFragmentTransaction.replace(R.id.right_drawer, mPageIndexFragment, TAG_FRAGMENT_PAGEINDEX);
            indexesFragmentTransaction.commitAllowingStateLoss();
        }
        mPageIndexFragment.init(retainDataFragment.getPaper());
    }

    private void loadContentFragment(String key, String position) {
        Log.v();
        IIndexItem indexItem = retainDataFragment.getPaper()
                                                 .getPlist()
                                                 .getIndexItem(key);
        if (indexItem != null) {
            switch (indexItem.getType()) {
                case ARTICLE:
                case TOPLINK:
                    loadArticleFragment(indexItem, DIRECTIONS.NONE, position);
                    break;
                case PAGE:
                    loadPagesFragment(indexItem);
                    break;
            }
        }
    }

    private void loadArticleFragment(String key, DIRECTIONS direction, String position) {
        IIndexItem indexItem = retainDataFragment.getPaper()
                                                 .getPlist()
                                                 .getIndexItem(key);
        loadArticleFragment(indexItem, direction, position);
    }

    private void loadArticleFragment(IIndexItem indexItem, DIRECTIONS direction, String position) {
        Log.v();

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        if (indexItem.getType() == IIndexItem.Type.TOPLINK) {
            mContentFragment = new TopLinkFragment();
        } else {
            mContentFragment = new ArticleFragment();
        }


        // important: Animation before replace!
        switch (direction) {
            case TOP:
                //fragmentTransaction.setCustomAnimations(R.anim.fragment_in_from_top, R.anim.fragment_out_to_down);
                fragmentTransaction.setCustomAnimations(R.animator.top_in, R.animator.bottom_out);
                break;
            case BOTTOM:
                //fragmentTransaction.setCustomAnimations(R.anim.fragment_in_from_down, R.anim.fragment_out_to_top);
                fragmentTransaction.setCustomAnimations(R.animator.bottom_in, R.animator.top_out);
                break;
            case LEFT:
                //fragmentTransaction.setCustomAnimations(R.anim.fragment_in_from_left, R.anim.fragment_out_to_right);
                fragmentTransaction.setCustomAnimations(R.animator.left_in, R.animator.right_out);
                break;
            case RIGHT:
                //fragmentTransaction.setCustomAnimations(R.anim.fragment_in_from_right, R.anim.fragment_out_to_left);
                fragmentTransaction.setCustomAnimations(R.animator.right_in, R.animator.left_out);
                break;
            default:
                break;
        }

        fragmentTransaction.replace(R.id.content_frame, mContentFragment);

        fragmentTransaction.commitAllowingStateLoss();

        mContentFragment.init(retainDataFragment.getPaper(), indexItem.getKey(), position);
    }

    private void loadPagesFragment(IIndexItem indexItem) {
        Log.v();
        if (indexItem.getType() == IIndexItem.Type.PAGE) {
            boolean needInit = false;
            if (mContentFragment == null) {
                mContentFragment = (AbstractContentFragment) mFragmentManager.findFragmentById(R.id.content_frame);
                needInit = true;
            }
            if (!(mContentFragment instanceof PagesFragment)) {
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                mContentFragment = new PagesFragment();
                fragmentTransaction.replace(R.id.content_frame, mContentFragment);
                fragmentTransaction.commitAllowingStateLoss();
                needInit = true;
            }
            if (needInit) mContentFragment.init(retainDataFragment.getPaper(), indexItem.getKey(), "");
            else ((PagesFragment) mContentFragment).setPage(indexItem.getKey());
        }
    }

    @Override
    public Loader<PaperLoader.PaperLoaderResult> onCreateLoader(int arg0, Bundle arg1) {
        Log.v();
        mLoadingProgress.setVisibility(View.VISIBLE);
        return new PaperLoader(this, paperId);
    }

    @Override
    public void onLoadFinished(Loader<PaperLoader.PaperLoaderResult> loader, final PaperLoader.PaperLoaderResult result) {
        Log.v();
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mLoadingProgress.setVisibility(View.GONE);
                if (!result.hasError()) {
                    if (result.getPaper() == null) {
                        result.setError(new NullPointerException("paper object is null"));
                    } else {
                        if (result.getPaper()
                                  .getPlist() == null) {
                            result.setError(new NullPointerException("plist object is null"));
                        }
                    }
                }
                if (!result.hasError()) {
                    Paper paper = result.getPaper();
                    retainDataFragment.setPaper(paper);
                    String currentKey = paper.getStoreValue(ReaderActivity.this, STORE_KEY_CURRENTPOSITION);
                    String position = paper.getStoreValue(ReaderActivity.this, STORE_KEY_POSITION_IN_ARTICLE);
                    if (Strings.isNullOrEmpty(currentKey)) {
                        currentKey = paper.getPlist()
                                          .getSources()
                                          .get(0)
                                          .getBooks()
                                          .get(0)
                                          .getCategories()
                                          .get(0)
                                          .getPages()
                                          .get(0)
                                          .getKey();
                    }
                    if (Strings.isNullOrEmpty(position)) position = "0";
                    retainDataFragment.setCurrentKey(ReaderActivity.this, currentKey, position);
                    initializeFragments();
                } else {
                    Log.e(result.getError());
                    Log.sendExceptionWithCrashlytics(result.getError());
                    ReaderActivity.this.finish();
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<PaperLoader.PaperLoaderResult> arg0) {
        Log.v();
    }

    @Override
    public boolean onLoadPrevArticle(DIRECTIONS fromDirection, String position) {
        Log.v();
        int prevPosition = retainDataFragment.getArticleCollectionOrderPosition(retainDataFragment.getCurrentKey()) - 1;

        if (retainDataFragment.filterBookmarks) {
            while (prevPosition >= 0) {
                IIndexItem item = retainDataFragment.getPaper()
                                                    .getPlist()
                                                    .getIndexItem(retainDataFragment.getArticleCollectionOrderKey(prevPosition));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                prevPosition--;
            }
        }

        if (prevPosition >= 0) {
            loadArticleFragment(retainDataFragment.getArticleCollectionOrderKey(prevPosition), fromDirection, position);
            return true;
        }
        return false;
    }

    @Override
    public boolean onLoadNextArticle(DIRECTIONS fromDirection, String position) {
        Log.v();
        int nextPositiion = retainDataFragment.getArticleCollectionOrderPosition(retainDataFragment.getCurrentKey()) + 1;

        if (retainDataFragment.filterBookmarks) {
            while (nextPositiion < retainDataFragment.getArticleCollectionSize()) {
                IIndexItem item = retainDataFragment.getPaper()
                                                    .getPlist()
                                                    .getIndexItem(retainDataFragment.getArticleCollectionOrderKey(nextPositiion));
                if (item != null) {
                    if (item.isBookmarked()) break;
                }
                nextPositiion++;
            }
        }

        if (nextPositiion < retainDataFragment.getArticleCollectionSize()) {

            loadArticleFragment(retainDataFragment.getArticleCollectionOrderKey(nextPositiion), fromDirection, position);
            return true;
        }
        return false;
    }


    public void setBackgroundColor(int color) {
        Log.d(color);
        this.findViewById(android.R.id.content)
            .setBackgroundColor(color);
    }

    @Override
    public int onGetBackgroundColor(String themeName) {
        THEMES theme = THEMES.valueOf(themeName);
        // THEMES theme = THEMES.valueOf(TazSettings.getPrefString(this, TazSettings.PREFKEY.THEME, "normal"));
        String hexColor = TazSettings.getPrefString(this, theme.getBgColorName(), "#FFFFFF");
        return Color.parseColor(hexColor);
    }

    @Override
    public boolean onLoad(String key) {
        loadContentFragment(key, "0");
        return false;
    }


    @Override
    public void onBookmarkClick(IIndexItem item) {
        Log.d(item.getKey());
        item.setBookmark(!item.isBookmarked());
        if (mIndexFragment != null) mIndexFragment.onBookmarkChange(item.getKey());
        if (item.getKey()
                .equals(retainDataFragment.getCurrentKey())) {
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
        // TODO Auto-generated method stub
    }

    @Override
    public void onDialogDismiss(String tag, Bundle arguments) {
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

    public void togglePageIndexDrawer() {
        if (mDrawerLayout.isDrawerOpen(mDrawerLayoutPageIndex)) {
            mDrawerLayout.closeDrawer(mDrawerLayoutPageIndex);
        } else {
            mDrawerLayout.openDrawer(mDrawerLayoutPageIndex);
        }

    }

    @Override
    public void onConfigurationChange(String name, String value) {
        Log.d(name, value);
        if (TazSettings.PREFKEY.THEME.equals(name)) setBackgroundColor(onGetBackgroundColor(value));
        callConfigListeners(name, value);
    }

    @Override
    public void onConfigurationChange(String name, boolean value) {
        Log.d(name, value);
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
    public void updateIndexes(String key, String position) {
        Log.d(key, System.currentTimeMillis());
        retainDataFragment.setCurrentKey(this, key, position);
        if (mIndexFragment != null) mIndexFragment.updateCurrentPosition(key);
        if (mPageIndexFragment != null) mPageIndexFragment.updateCurrentPosition(key);
        if (mContentFragment instanceof PagesFragment) ((PagesFragment) mContentFragment).setShareButtonCallback(retainDataFragment.getPaper()
                                                                                                                                   .getPlist()
                                                                                                                                   .getIndexItem(key));
        Log.d(key, System.currentTimeMillis());
    }


    @Override
    public String getCurrentKey() {
        return retainDataFragment.getCurrentKey();
    }

    @Override
    public String getStoreValue(String path, String value) {
        String result = Store.getValueForKey(this, "/" + getPaper().getBookId() + "/" + path);
        Log.d(path, result);
        return result;
    }

    @Override
    public void setFilterBookmarks(boolean bool) {
        retainDataFragment.filterBookmarks = bool;
    }

    @Override
    public boolean isFilterBookmarks() {
        return retainDataFragment.filterBookmarks;
    }

    public static class RetainDataFragment extends Fragment {

        private static final String TAG = "RetainDataFragment";

        private Paper _paper;
        private String mCurrentKey;
        private String mPosition;
        private boolean filterBookmarks;

        private HashMap<String, Integer> articleCollectionOrder = new HashMap<>();
        private HashMap<Integer, String> articleCollectionPositionIndex = new HashMap<>();

        public RetainDataFragment() {
        }

        public static RetainDataFragment findRetainFragment(FragmentManager fm) {
            return (RetainDataFragment) fm.findFragmentByTag(TAG);
        }

        public static RetainDataFragment createRetainFragment(FragmentManager fm) {
            RetainDataFragment fragment = new RetainDataFragment();
            fm.beginTransaction()
              .add(fragment, TAG)
              .commit();
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public Paper getPaper() {
            return _paper;
        }

        public void setPaper(Paper paper) {
            this._paper = paper;
            //            articleItems = new ArrayList<>();

            //Reihenfolge der Artikel festlegen
            List<TopLink> toplinksToSortIn = new ArrayList<>(paper.getPlist()
                                                                  .getToplinks());
            int position = 0;
            for (Source source : paper.getPlist()
                                      .getSources()) {
                for (Book book : source.getBooks()) {
                    for (Category category : book.getCategories()) {
                        for (Page page : category.getPages()) {

                            Iterator<TopLink> i = toplinksToSortIn.iterator();


                            while (i.hasNext()) {
                                TopLink topLink = i.next();
                                if (!topLink.isLink()) {
                                    if (topLink.getKey()
                                               .equals(page.getDefaultLink())) {
                                        articleCollectionOrder.put(topLink.getKey(), position);
                                        articleCollectionPositionIndex.put(position, topLink.getKey());
                                        position++;
                                        //articleItems.add(topLink);
                                        i.remove();
                                    }
                                }
                            }
                            for (Article article : page.getArticles()) {
                                if (!article.isLink()) {
                                    articleCollectionOrder.put(article.getKey(), position);
                                    articleCollectionPositionIndex.put(position, article.getKey());
                                    position++;
                                    //articleItems.add(article);
                                }
                            }
                        }
                    }
                }
            }
            for (TopLink topLink : toplinksToSortIn) {
                if (!topLink.isLink()) {
                    articleCollectionOrder.put(topLink.getKey(), position);
                    articleCollectionPositionIndex.put(position, topLink.getKey());
                    position++;
                }
            }

            //            articleItems.addAll(toplinksToSortIn);
        }

        public int getArticleCollectionOrderPosition(String key) {
            return articleCollectionOrder.get(key);
        }

        public int getArticleCollectionSize() {
            return articleCollectionOrder.size();
        }

        public String getArticleCollectionOrderKey(int postion) {
            return articleCollectionPositionIndex.get(postion);
        }

        public void setCurrentKey(Context context, String currentKey, String position) {
            Log.d(currentKey, position);
            mCurrentKey = currentKey;
            mPosition = position;
            try {
                _paper.saveStoreValue(context, STORE_KEY_CURRENTPOSITION, mCurrentKey);
                _paper.saveStoreValue(context, STORE_KEY_POSITION_IN_ARTICLE, position);
            } catch (Exception e) {
                Log.w(e);
            }

            boolean addtobackstack = true;
            BackStack newBackStack = new BackStack(currentKey, position);
            if (backstack.size() > 0) {
                BackStack lastBackstack = backstack.get(backstack.size() - 1);
                if (lastBackstack.equals(newBackStack)) addtobackstack = false;
            }
            if (addtobackstack) {
                Log.d("Adding to backstack", currentKey, position);
                backstack.add(newBackStack);
            }
        }

        public String getCurrentKey() {
            return mCurrentKey;
        }

        public String getPostion() {
            return mPosition;
        }

        ArrayList<BackStack> backstack = new ArrayList<>();

        public class BackStack {
            String key;
            String position;

            public BackStack(String key, String position) {
                Log.d(key, position);
                this.key = key;
                this.position = position;
            }

            public String getKey() {
                return key;
            }

            public String getPosition() {
                return position;
            }

            @Override
            public boolean equals(Object other) {
                if (other instanceof BackStack) {
                    BackStack otherBS = (BackStack) other;
                    boolean keyCompare = false;
                    boolean positionCompare = false;

                    if (key == null && otherBS.key == null) keyCompare = true;
                    else if (key != null) {
                        if (key.equals(otherBS.key)) keyCompare = true;
                    }
                    if (position == null && otherBS.position == null) positionCompare = true;
                    else if (position != null) {
                        if (position.equals(otherBS.position)) positionCompare = true;
                    }
                    if (keyCompare && positionCompare) return true;

                }
                return false;
            }
        }

        public ArrayList<BackStack> getBackstack() {
            return backstack;
        }
    }

    @Override
    public Paper getPaper() {
        return retainDataFragment.getPaper();
    }


    @Override
    public void onBackPressed() {
        if (retainDataFragment != null && retainDataFragment.getBackstack() != null && retainDataFragment.getBackstack()
                                                                                                         .size() > 1) {
            RetainDataFragment.BackStack loadBackStack = retainDataFragment.getBackstack()
                                                                           .get(retainDataFragment.getBackstack()
                                                                                                  .size() - 2);
            retainDataFragment.getBackstack()
                              .remove(retainDataFragment.getBackstack()
                                                        .size() - 1);
            retainDataFragment.getBackstack()
                              .remove(retainDataFragment.getBackstack()
                                                        .size() - 1);
            loadContentFragment(loadBackStack.getKey(), loadBackStack.getPosition());
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d();
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                togglePageIndexDrawer();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void setImmersiveMode() {

        boolean onOff = TazSettings.getPrefBoolean(this,TazSettings.PREFKEY.FULLSCREEN,false);

        Log.v("immersive:"+onOff);


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
}
