package de.thecode.android.tazreader.reader;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import de.thecode.android.tazreader.data.Paper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 13.11.2015.
 */
public class ReaderDataFragment extends Fragment /*implements TextToSpeech.OnInitListener*/ {

    private static final String TAG = "RetainDataFragment";

    private Paper _paper;
    private String mCurrentKey;
    private String mPosition;
    private boolean filterBookmarks;

    private HashMap<String, Integer> articleCollectionOrder = new HashMap<>();
    private HashMap<Integer, String> articleCollectionPositionIndex = new HashMap<>();

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
        return callback != null && callback.get() != null;
    }

    private ReaderDataFramentCallback getCallback() {
        return callback.get();
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
        Timber.d("%s %s", context, currentKey, position);

        mCurrentKey = currentKey;
        mPosition = position;
        try {
            _paper.saveStoreValue(context, ReaderActivity.STORE_KEY_CURRENTPOSITION, mCurrentKey);
            _paper.saveStoreValue(context, ReaderActivity.STORE_KEY_POSITION_IN_ARTICLE, position);
        } catch (Exception e) {
            Timber.w(e);
        }

        boolean addtobackstack = true;
        BackStack newBackStack = new BackStack(currentKey, position);
        if (backstack.size() > 0) {
            BackStack lastBackstack = backstack.get(backstack.size() - 1);
            if (lastBackstack.equals(newBackStack)) addtobackstack = false;
        }
        if (addtobackstack) {
            Timber.d("%s", currentKey, position);
            //Log.d("Adding to backstack", currentKey, position);
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
            Timber.d("key: %s", key, position);
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



    public interface ReaderDataFramentCallback {
    }
}
