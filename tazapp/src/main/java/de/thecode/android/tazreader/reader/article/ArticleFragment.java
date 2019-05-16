package de.thecode.android.tazreader.reader.article;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ParseException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.reader.AbstractContentFragment;
import de.thecode.android.tazreader.reader.ReaderActivity;
import de.thecode.android.tazreader.reader.ReaderActivity.DIRECTIONS;
import de.thecode.android.tazreader.reader.ReaderBaseFragment;
import de.thecode.android.tazreader.reader.article.ArticleWebView.ArticleWebViewCallback;
import de.thecode.android.tazreader.utils.Charsets;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.TintHelper;
import de.thecode.android.tazreader.widget.ShareButton;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import kotlin.io.FilesKt;
import timber.log.Timber;

public class ArticleFragment extends AbstractContentFragment implements ArticleWebViewCallback {

    private static final String JAVASCRIPT_API_NAME = "ANDROIDAPI";

    static final         String ARG_KEY      = "arg_article_key";
    private static final String ARG_POSITION = "arg_article_position";

    public static ArticleFragment newInstance(String bookId, String articleKey, String position) {
        ArticleFragment fragment = ReaderBaseFragment.newInstance(ArticleFragment.class, bookId);
        Bundle arguments = fragment.getArguments();
        arguments.putString(ARG_KEY, articleKey);
        arguments.putString(ARG_POSITION, position);
        fragment.setArguments(arguments);
        return fragment;
    }


    private enum GESTURES {
        undefined, swipeUp, swipeDown, swipeRight, swipeLeft
    }

    ArticleWebView mWebView;
    ProgressBar    mProgressBar;
    FrameLayout    mBookmarkClickLayout;
    ShareButton    mShareButton;
    ImageView      playButton;


    Handler  mUiThreadHandler;
    GESTURES mLastGesture = GESTURES.undefined;

    ArticleViewModel articleViewModel;

    public ArticleFragment() {
        super();
        mUiThreadHandler = new Handler(Looper.getMainLooper());
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        articleViewModel = ViewModelProviders.of(this,
                                                 new ArticleViewModelFactory(getReaderViewModel(),
                                                                             getArguments().getString(ARG_KEY),
                                                                             getArguments().getString(ARG_POSITION)))
                                             .get(ArticleViewModel.class);

    }

    @SuppressLint({"SetJavaScriptEnabled", "NewApi", "AddJavascriptInterface"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.reader_article, container, false);

        mBookmarkClickLayout = result.findViewById(R.id.bookmarkClickLayout);

        mWebView = result.findViewById(R.id.webview);
        mWebView.setAlpha(0F);
        mWebView.setArticleWebViewCallback(this);

        mWebView.setBackgroundColor(getReaderActivity().onGetBackgroundColor(TazSettings.getInstance(getContext())
                                                                                        .getPrefString(TazSettings.PREFKEY.THEME,
                                                                                                       "normal")));

        mWebView.setWebViewClient(new ArticleWebViewClient());
        mWebView.setWebChromeClient(new ArticleWebChromeClient());
        WebSettings webviewSettings = mWebView.getSettings();
        webviewSettings.setAllowFileAccessFromFileURLs(true);
        webviewSettings.setJavaScriptEnabled(true);
        webviewSettings.setBuiltInZoomControls(true);
        webviewSettings.setSupportZoom(true);
        webviewSettings.setUseWideViewPort(true);

        mWebView.addJavascriptInterface(new ANDROIDAPI(), JAVASCRIPT_API_NAME);


        //fade(0F, 0);

        mWebView.setHorizontalScrollBarEnabled(true);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setScrollbarFadingEnabled(true);

        mProgressBar = result.findViewById(R.id.progressBar);

        mShareButton = result.findViewById(R.id.share);
        playButton = result.findViewById(R.id.play);
        playButton.setVisibility(View.GONE);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getReaderActivity().speak2((Article) articleViewModel.getTocItemLiveData().getValue());
            }
        });

        ImageView mPageIndexButton = result.findViewById(R.id.pageindex);
        if (TazSettings.getInstance(getContext())
                       .getPrefBoolean(TazSettings.PREFKEY.PAGEINDEXBUTTON, false)) {
            mPageIndexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openPageIndexDrawer();
                    }
                }
            });
            mPageIndexButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(v.getContext(), R.string.reader_action_pageindex, Toast.LENGTH_LONG)
                         .show();
                    return true;
                }
            });
        } else mPageIndexButton.setVisibility(View.GONE);

        ImageView mIndexButton = result.findViewById(R.id.index);
        if (TazSettings.getInstance(getContext())
                       .isIndexButton()) {
            mIndexButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() instanceof ReaderActivity) {
                        ((ReaderActivity) getActivity()).openIndexDrawer();
                    }
                }
            });
            mIndexButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(v.getContext(), R.string.reader_action_index, Toast.LENGTH_LONG)
                         .show();
                    return true;
                }
            });
        } else mIndexButton.setVisibility(View.GONE);

        //ttsActive = TazSettings.getPrefBoolean(getContext(), TazSettings.PREFKEY.TEXTTOSPEACH, false);


        return (result);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        articleViewModel.getTocItemLiveData()
                        .observe(this, new Observer<ITocItem>() {
                            @Override
                            public void onChanged(ITocItem iTocItem) {
//                mArticle = iTocItem;
                                initialBookmark();
                                loadArticleInWebView();
                                if (iTocItem instanceof Article) {
                                    if (!TextUtils.isEmpty(((Article) iTocItem).getAudiolink())) {
                                        playButton.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        });
//        articleViewModel.getPlayerButtonVisiblityLiveData()
//                        .observe(this, visible -> playButton.setVisibility(visible ? View.VISIBLE : View.GONE));
//        getTtsViewModel().getPlayerVisibleLiveData()
//                         .observe(this, new Observer<Boolean>() {
//                             @Override
//                             public void onChanged(Boolean visible) {
//                                 playButton.setVisibility(visible ? View.GONE : View.VISIBLE);
//                             }
//                         });
    }


    private void runOnUiThread(Runnable runnable) {
        mUiThreadHandler.post(runnable);
    }

    private void loadArticleInWebView() {
        String baseUrl = "file://" + getReaderViewModel().getPaperDirectory() + "/" + articleViewModel.getKey() + "?position=" + articleViewModel.getPosition();
        mWebView.loadDataWithBaseURL(baseUrl, getHtml(), "text/html", "UTF-8", null);
        mShareButton.setCallback(articleViewModel.getTocItemLiveData()
                                                 .getValue());
    }


    public void callTazapi(String methodname, Object... params) {

        StringBuilder jsBuilder = new StringBuilder();
        jsBuilder.append("TAZAPI")
                 .append(".")
                 .append(methodname)
                 .append("(");
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param instanceof String) {
                jsBuilder.append("'");
                jsBuilder.append(param);
                jsBuilder.append("'");
            } else jsBuilder.append(param);
            if (i < params.length - 1) {
                jsBuilder.append(",");
            }
        }
        jsBuilder.append(");");
        String call = jsBuilder.toString();
        mUiThreadHandler.post(new Runnable() {

            String call;

            @Override
            public void run() {
                Timber.i("Calling javascript with %s", call);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    mWebView.evaluateJavascript(call, null);
                } else {
                    mWebView.loadUrl("javascript:" + call);
                }
            }

            public Runnable setCall(String call) {
                this.call = call;
                return this;
            }
        }.setCall(call));
    }


    private void onGestureToTazapi(GESTURES gesture, MotionEvent e1) {
        //mOpenGesureResult = true;
        //callTazapi("onGesture", "'" + gesture.name() + "'," + e1.getX() + "," + e1.getY());
        callTazapi("onGesture", gesture.name(), e1.getX(), e1.getY());
    }

    @Override
    public void onSwipeLeft(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        mLastGesture = GESTURES.swipeLeft;
        onGestureToTazapi(mLastGesture, e1);
    }

    @Override
    public void onSwipeRight(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        mLastGesture = GESTURES.swipeRight;
        onGestureToTazapi(mLastGesture, e1);
    }

    @Override
    public void onSwipeTop(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        mLastGesture = GESTURES.swipeUp;
        onGestureToTazapi(mLastGesture, e1);
    }

    @Override
    public void onSwipeBottom(ArticleWebView view, MotionEvent e1, MotionEvent e2) {

        mLastGesture = GESTURES.swipeDown;
        onGestureToTazapi(mLastGesture, e1);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (getReaderActivity() != null) {
            getReaderActivity().speak(articleViewModel.getKey(), getTextToSpeech());
        }
        return true;
    }

    @Override
    public void onConfigurationChange(String key, String value) {
        Timber.d("%s %s", key, value);
        callTazapi("onConfigurationChanged", key, value);
    }

    @Override
    public void onScrollStarted(ArticleWebView view) {
        Timber.d("%s %s", view.getScrollX(), view.getScrollY());
    }

    @Override
    public void onScrollFinished(ArticleWebView view) {
        Timber.d("%s %s", view.getScrollX(), view.getScrollY());
    }


    public void initialBookmark() {
        ITocItem mArticle = articleViewModel.getTocItemLiveData()
                                            .getValue();
        if (!(mArticle instanceof Article)) mBookmarkClickLayout.setVisibility(View.GONE);
        else {
            mBookmarkClickLayout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    getReaderActivity().onBookmarkClick(mArticle);
                    //if (callback != null) callback.onBookmarkClick(mArticle);
                }
            });

            ImageView bookmark = mBookmarkClickLayout.findViewById(R.id.bookmark);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) bookmark.getLayoutParams();
            if (mArticle.isBookmarked()) {
                TintHelper.tintDrawable(bookmark.getDrawable(), ContextCompat.getColor(getActivity(), R.color.index_bookmark_on));
                bookmark.setAlpha(1F);
                layoutParams.topMargin = getContext().getResources()
                                                     .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_active);
            } else {
                TintHelper.tintDrawable(bookmark.getDrawable(),
                                        ContextCompat.getColor(getActivity(), R.color.index_bookmark_off));
                TypedValue outValue = new TypedValue();
                getContext().getResources()
                            .getValue(R.dimen.icon_button_alpha, outValue, true);
                bookmark.setAlpha(outValue.getFloat());
                layoutParams.topMargin = getContext().getResources()
                                                     .getDimensionPixelOffset(R.dimen.reader_bookmark_offset_normal);
            }
            bookmark.setLayoutParams(layoutParams);
        }
    }


    public class ArticleWebChromeClient extends WebChromeClient {

        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {

            StringBuilder messagBuilder = new StringBuilder(consoleMessage.messageLevel()
                                                                          .toString()).append(" in ")
                                                                                      .append(consoleMessage.sourceId())
                                                                                      .append(" on line ")
                                                                                      .append(consoleMessage.lineNumber())
                                                                                      .append(": ")
                                                                                      .append(consoleMessage.message());

            switch (consoleMessage.messageLevel()) {
                case TIP:
                    Timber.i("%s %s", articleViewModel.getKey(), messagBuilder.toString());
                    break;
                case WARNING:
                    Timber.w("%s %s", articleViewModel.getKey(), messagBuilder.toString());
                    break;
                case DEBUG:
                    Timber.d("%s %s", articleViewModel.getKey(), messagBuilder.toString());
                    break;
                case ERROR:
                    Timber.e("%s %s", articleViewModel.getKey(), messagBuilder.toString());
                    break;
                case LOG:
                    Timber.i("%s %s", articleViewModel.getKey(), messagBuilder.toString());
                    break;
            }
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            Timber.d("%s %s %s", message, url, result.toString());

            return true;
        }

    }

    public class ANDROIDAPI {

        @JavascriptInterface
        public void openUrl(final String url) {
            Timber.d("%s %s", articleViewModel.getKey(), url);
            if (url.startsWith("http")) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else if (url.startsWith("mailto:")) {
                try {
                    //MailTo mt = MailTo.parse(url);
                    Intent i = new Intent(Intent.ACTION_SENDTO);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (ParseException e) {
                    Toast.makeText(getContext(), "Kein gültiger RFC 2368 mailto: Link\n" + url, Toast.LENGTH_LONG)
                         .show();
                }
            } else if (url.startsWith(articleViewModel.getKey()) || url.startsWith("?")) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadArticleInWebView();
                    }
                });

            } else {
                getReaderActivity().loadContentFragment(url);
                //if (callback != null) callback.onLoad(url);
            }

        }

        @JavascriptInterface
        public String getValue(String path) {
            Timber.d("%s", path);
            Store store = getReaderViewModel().getStoreRepository()
                                              .getStoreForPath(path);
            String result = store.getValue();
            Timber.d("%s %s %s", articleViewModel.getKey(), path, result);
            return result;
        }

        @JavascriptInterface
        public boolean setValue(String path, String value) {
            Timber.d("%s=%s", path, value);
            if (path != null && !path.contains("/" + Paper.STORE_KEY_CURRENTPOSITION)) {
                Store store = new Store(path, value);
                getReaderViewModel().getStoreRepository()
                                    .saveStore(store);
            }
//            if (store == null) {
//                store = new Store(path, value);
//                Uri resultUri = getContext().getContentResolver()
//                                            .insert(Store.CONTENT_URI, store.getContentValues());
//                if (resultUri != null) result = true;
//            } else {
//                store.setValue(value);
//                int affected = getContext().getContentResolver()
//                                           .update(Store.getUriForKey(path), store.getContentValues(), null, null);
//                if (affected > 0) result = true;
//            }
//            Timber.d("%s %s %s", mArticle.getKey(), path, value, result);
            return true;
        }

        @JavascriptInterface
        public String getConfiguration(String name) {
            String result = getConfig(name);
            Timber.d("%s %s %s", articleViewModel.getKey(), name, result);
            return result;
        }

        @JavascriptInterface
        public boolean setConfiguration(String name, String value) {
            boolean result = setConfig(name, value);
            Timber.d("%s %s %s %s", articleViewModel.getKey(), name, value, result);
            return result;
        }

        @JavascriptInterface
        public void pageReady(String percentSeen, String position, String numberOfPages) {
            Timber.d("%s %s %s %s", articleViewModel.getKey(), percentSeen, position, numberOfPages);
//            Store positionStore = getReaderViewModel().getStoreRepository()
//                                                      .getStore(getReaderViewModel().getPaper()
//                                                                                    .getBookId(),
//                                                                Paper.STORE_KEY_POSITION_IN_ARTICLE + "_" + key);
//            positionStore.setValue(position);
//            getReaderViewModel().getStoreRepository()
//                                .saveStore(positionStore);
            articleViewModel.setPosition(position);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.animate()
                            .alpha(1F)
                            .setDuration(400)
                            .start();
                    mProgressBar.animate()
                                .alpha(0F)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mProgressBar.setVisibility(View.GONE);
                                    }
                                })
                                .setDuration(400)
                                .start();
                    //mProgressBar.setVisibility(View.GONE);
                }
            });
        }

        @JavascriptInterface
        public void enableRegionScroll(boolean isOn) {
            Timber.d("%s %s", articleViewModel.getKey(), isOn);
        }

        @JavascriptInterface
        public void beginRendering() {
            Timber.d(articleViewModel.getKey());
        }

        @JavascriptInterface
        public void nextArticle(final int position) {
            Timber.d("%s %s", articleViewModel.getKey(), position);
            //mAnimationLock = true;
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    DIRECTIONS direction;
                    switch (mLastGesture) {
                        case swipeUp:
                            direction = DIRECTIONS.BOTTOM;
                            break;
                        case swipeDown:
                            direction = DIRECTIONS.TOP;
                            break;
                        case swipeRight:
                            direction = DIRECTIONS.LEFT;
                            break;
                        default:
                            direction = DIRECTIONS.RIGHT;
                            break;
                    }

                    if (getReaderActivity() != null) getReaderActivity().onLoadNextArticle(direction, String.valueOf(position));
                }
            });

        }

        @JavascriptInterface
        public void previousArticle(final int position) {
            Timber.d("%s %s", articleViewModel.getKey(), position);

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    String positionString = String.valueOf(position);
                    DIRECTIONS direction;

                    switch (mLastGesture) {
                        case swipeUp:
                            direction = DIRECTIONS.BOTTOM;
                            break;
                        case swipeDown:
                            direction = DIRECTIONS.TOP;
                            break;
                        case swipeLeft:
                            direction = DIRECTIONS.RIGHT;
                            break;
                        default:
                            if (!TazSettings.getInstance(getContext())
                                            .getPrefBoolean(TazSettings.PREFKEY.ISSCROLL, false)) positionString = "EOF";
                            direction = DIRECTIONS.LEFT;
                            break;
                    }

                    if (getReaderActivity() != null) getReaderActivity().onLoadPrevArticle(direction, positionString);
                }
            });
        }

        @JavascriptInterface
        public String getAbsoluteResourcePath() {
            File resourceDir = StorageManager.getInstance(getContext())
                                             .getResourceDirectory(getReaderViewModel().getResource());
            return "file://" + resourceDir.getAbsolutePath() + "/";
        }

        @JavascriptInterface
        public void clearWebCache() {


        }
    }

    public class ArticleWebViewClient extends WebViewClient {

        @Override
        public void onLoadResource(WebView view, String url) {
            Timber.i(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Timber.i(url);
            if (url != null) {
                if (url.toLowerCase(Locale.getDefault())
                       .startsWith("http")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } else {
//                    if (callback != null) {
                    ITocItem indexItem = getReaderViewModel().getPaper()
                                                             .getPlist()
                                                             .getIndexItem(url);
                    if (indexItem != null && getReaderActivity() != null) {
                        getReaderActivity().loadContentFragment(url);
                        //callback.onLoad(url);
                    }
//                    }
                }
            }
            return true;
        }


        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Timber.e("%s errorCode: %d", articleViewModel.getKey(), errorCode);
            Timber.e("%s description: %s", articleViewModel.getKey(), description);
            Timber.e("%s failingUrl: %s", articleViewModel.getKey(), failingUrl);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    private static final String TAZAPI_FILE_STRING = "TAZAPI.js";


    public String getHtml() {
        File articleFile = new File(getReaderViewModel().getPaperDirectory(), articleViewModel.getKey());
        File resourceDir = getReaderViewModel().getResourceDirectory();

        String resourceReplacement = "file://" + resourceDir.getAbsolutePath() + "/";
        String tazapiReplacement = "file:///android_asset/js/TAZAPI.js";

        String result = null;

        result = FilesKt.readText(articleFile, Charsets.UTF_8);
//            result = FileUtilsOld.readFile(articleFile, Charsets.UTF_8);
//            result = Files.asCharSource(articleFile, Charsets.UTF_8).read();
//            result = IOUtils.toString(new FileInputStream(articleFile), "UTF-8");

        //            Pattern tazapiPattern = Pattern.compile("(<script.+?src\\s*?=\\s*?(?:\"|'))(res.+?TAZAPI.js)((?:\"|').*?>)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        //            Matcher matcher = tazapiPattern.matcher(result);
        //            result = matcher.replaceAll("$1" + tazapiReplacement + "$3");

        Pattern resPattern = Pattern.compile("(<[^>]+?(?:href|src)\\s*?=\\s*?(?:\"|'))(res.+?)((?:\"|').*?>)",
                                             Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = resPattern.matcher(result);
        result = matcher.replaceAll("$1" + resourceReplacement + "$2$3");

        Pattern tazapiPattern = Pattern.compile("file://.+?TAZAPI.js", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = tazapiPattern.matcher(result);
        result = matcher2.replaceAll(tazapiReplacement);


        if (TextUtils.isEmpty(result)) result = "Fehler beim Laden des Artikels";

        return result;

        // (\<[^\>]+?(?:href|src)\=(?:\"|\'))(res)(.+?(?:\"|\')\>)
    }

//    @Override
//    public void onTtsStateChanged(ReaderTTSViewModel.TTS state) {
//        Timber.d("%s", state);
//    }

    private CharSequence getTextToSpeech() {
        Pattern pattern = Pattern.compile(".*?<body.*?>(.*?)</body>.*?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(getHtml());
        if (matcher.matches()) {

            Pattern replacePattern = Pattern.compile("[\u00AD]?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher replaceMatcher = replacePattern.matcher(Html.fromHtml(matcher.group(1)));
            return replaceMatcher.replaceAll("");
        }
        return null;
    }
}
