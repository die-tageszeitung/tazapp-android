package de.thecode.android.tazreader.reader;


import android.text.TextUtils;

import de.mateware.datafragment.DataFragmentBase;
import de.thecode.android.tazreader.data.Paper;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

/**
 * Created by mate on 13.11.2015.
 */
public class ReaderDataFragment extends DataFragmentBase {

    private Paper   _paper;
    private String  mCurrentKey;
    //private String  mPosition;
    private boolean filterBookmarks;

    private PaperLoadingTask paperLoadingTask;

    public Paper getPaper() {
        return _paper;
    }

    public void setPaper(Paper paper) {
        this._paper = paper;
    }

    public boolean isPaperLoaded() {
        return _paper != null;
    }

    public void loadPaper(long paperId) {
        if (paperLoadingTask == null) {
            paperLoadingTask = new PaperLoadingTask(getContext(), paperId) {
                @Override
                protected void onPostError(Exception exception) {
                    EventBus.getDefault().post(new PaperLoadedEvent(exception));
                }
                @Override
                protected void onPostSuccess(Paper paper) {
                    _paper = paper;
                    if (!isCancelled()) {
                        String currentKey = paper.getStoreValue(getContext(), ReaderActivity.STORE_KEY_CURRENTPOSITION);
                        currentKey = StringUtils.substringBefore(currentKey,"?"); //Workaround for sometimes position saved in key, could ot figure out why
                        //String position = paper.getStoreValue(getContext(), ReaderActivity.STORE_KEY_POSITION_IN_ARTICLE);
                        if (TextUtils.isEmpty(currentKey)) {
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
                        //if (TextUtils.isEmpty(position)) position = "0";
                        setCurrentKey(currentKey);
                        EventBus.getDefault()
                                .post(new PaperLoadedEvent());
                    }
                }
            };
            paperLoadingTask.execute();
        }
    }

    public void setCurrentKey(String currentKey) {
        //Timber.d("%s %s", currentKey, position);

        mCurrentKey = currentKey;
        //mPosition = position;
        try {
            _paper.saveStoreValue(getContext(), ReaderActivity.STORE_KEY_CURRENTPOSITION, mCurrentKey);
            //_paper.saveStoreValue(getContext(), ReaderActivity.STORE_KEY_POSITION_IN_ARTICLE, position);
        } catch (Exception e) {
            Timber.w(e);
        }
    }

    public String getCurrentKey() {
        return mCurrentKey;
    }

//    public String getPostion() {
//        return mPosition;
//    }

    public boolean isFilterBookmarks() {
        return filterBookmarks;
    }

    public void setFilterBookmarks(boolean filterBookmarks) {
        this.filterBookmarks = filterBookmarks;
    }

    @Override
    public void onDestroy() {
        if (paperLoadingTask != null) paperLoadingTask.cancel(true);
        super.onDestroy();
    }
}
