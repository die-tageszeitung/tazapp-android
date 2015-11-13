package de.thecode.android.tazreader.reader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 13.11.2015.
 */
public class ReaderDataFragment extends Fragment implements TextToSpeech.OnInitListener{

    private static final String TAG = "RetainDataFragment";

    public enum TTS {DISABLED, WAITING, PLAYING}

    private Paper _paper;
    private String mCurrentKey;
    private String mPosition;
    private boolean filterBookmarks;

    private HashMap<String, Integer> articleCollectionOrder = new HashMap<>();
    private HashMap<Integer, String> articleCollectionPositionIndex = new HashMap<>();

    private TextToSpeech tts;
    private TTS ttsState = TTS.DISABLED;

    private WeakReference<ReaderDataFramentCallback> callback;

    public ReaderDataFragment() {
    }

    public static ReaderDataFragment findRetainFragment(FragmentManager fm) {
        return (ReaderDataFragment) fm.findFragmentByTag(TAG);
    }

    public static ReaderDataFragment createRetainFragment(FragmentManager fm) {
        ReaderDataFragment fragment = new ReaderDataFragment();
        fm.beginTransaction()
          .add(fragment, TAG)
          .commit();
        return fragment;
    }

    public void setCallback(ReaderDataFramentCallback callback) {
        this.callback = new WeakReference<>(callback);
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private ReaderDataFramentCallback getCallback() {
        return callback.get();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        Log.d(status);
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.GERMAN);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                setTtsState(TTS.DISABLED);
            } else {
                tts.setOnUtteranceProgressListener(ttsListener);
                setTtsState(TTS.WAITING);
            }
        }
    }

    public void initTts(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    private void setTtsState(@NonNull TTS newState) {
        TTS oldState = ttsState;
        ttsState = newState;
        if (!newState.equals(oldState)) {
            if (hasCallback()) getCallback().onTtsStateChanged(ttsState);
        }
    }

    public TTS getTtsState() {
        return ttsState;
    }

    public Paper getPaper() {
        return _paper;
    }

    public void setPaper(Paper paper) {
        this._paper = paper;
        //            articleItems = new ArrayList<>();

        //Reihenfolge der Artikel festlegen
        List<Paper.Plist.TopLink> toplinksToSortIn = new ArrayList<>(paper.getPlist()
                                                                          .getToplinks());
        int position = 0;
        for (Paper.Plist.Source source : paper.getPlist()
                                              .getSources()) {
            for (Paper.Plist.Book book : source.getBooks()) {
                for (Paper.Plist.Category category : book.getCategories()) {
                    for (Paper.Plist.Page page : category.getPages()) {

                        Iterator<Paper.Plist.TopLink> i = toplinksToSortIn.iterator();


                        while (i.hasNext()) {
                            Paper.Plist.TopLink topLink = i.next();
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
                        for (Paper.Plist.Page.Article article : page.getArticles()) {
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
        for (Paper.Plist.TopLink topLink : toplinksToSortIn) {
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
            _paper.saveStoreValue(context, ReaderActivity.STORE_KEY_CURRENTPOSITION, mCurrentKey);
            _paper.saveStoreValue(context, ReaderActivity.STORE_KEY_POSITION_IN_ARTICLE, position);
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


    public boolean isFilterBookmarks() {
        return filterBookmarks;
    }

    public void setFilterBookmarks(boolean filterBookmarks) {
        this.filterBookmarks = filterBookmarks;
    }

    public void speak(CharSequence text) {

        switch (ttsState) {
            case DISABLED:
                break;
            case PLAYING:
                tts.stop();
                if (hasCallback()) getCallback().showToast(R.string.toast_tts_stopped, Toast.LENGTH_LONG);
                setTtsState(ReaderDataFragment.TTS.WAITING);
                break;
            case WAITING:
                if (hasCallback()) getCallback().showToast(R.string.toast_tts_started, Toast.LENGTH_LONG);
                speak(text, false);
                break;
        }
    }

    private void speak(CharSequence text, boolean enqueue){
        int enqueueType = enqueue ? TextToSpeech.QUEUE_ADD : TextToSpeech.QUEUE_FLUSH;

        int maxLength = 2000;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            maxLength = TextToSpeech.getMaxSpeechInputLength();
        }

        CharSequence laterText = null;
        if (maxLength < text.length()) {
            String[] textArray = text.toString().split("\\n\\n", 2);
            text = textArray[0];
            if (textArray.length > 1)
                laterText = textArray[1];
        }

        String utteranceId = String.valueOf(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, enqueueType, null, utteranceId);
        } else {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.speak(text.toString(), enqueueType, map);
        }

        if (!TextUtils.isEmpty(laterText))
            speak(laterText,true);
    }

    UtteranceProgressListener ttsListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            setTtsState(TTS.PLAYING);
        }

        @Override
        public void onDone(String utteranceId) {
            setTtsState(TTS.WAITING);
        }

        @Override
        public void onError(String utteranceId) {
            setTtsState(TTS.WAITING);
        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            setTtsState(TTS.WAITING);
        }
    };

    public interface ReaderDataFramentCallback {
        void onTtsStateChanged(TTS newState);
        void showToast(@StringRes int stringId, int toastLenght);
    }
}
