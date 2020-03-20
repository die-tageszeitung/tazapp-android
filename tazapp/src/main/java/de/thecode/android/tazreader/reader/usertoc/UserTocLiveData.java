package de.thecode.android.tazreader.reader.usertoc;

import android.annotation.SuppressLint;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.core.util.Pair;

import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.ParametrizedRunnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by mate on 22.03.18.
 */
public class UserTocLiveData extends LiveData<UserTocLiveData.ResultWrapper> {

    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());

    private final Map<String, UserTocItem> userTocMap = new LinkedHashMap<>();
    private final LiveData<ITocItem> currentKeyLiveData;
    private       boolean            filterBookmarks;
    private       boolean            expanded;

    private final Observer<ITocItem> currentKeyObserver = tocItem ->
        executor.execute(new ParametrizedRunnable<ITocItem>() {
            @Override
            public void run(ITocItem tocItem) {
                if (!expanded) expandAllInternal(false);
                if (tocItem instanceof Paper.Plist.Page) tocItem = tocItem.getIndexParent();
                expandInternal(tocItem, true);
                for (Map.Entry<String, UserTocItem> userTocItemEntry : userTocMap.entrySet()) {
                    UserTocItem userTocItem = userTocItemEntry.getValue();
                    if (userTocItem != null) {
                        userTocItem.setActive(tocItem.getKey()
                                                     .equals(userTocItem.getKey()));
                    }
                }
                publish(true);
            }
        }.set(tocItem));

    public UserTocLiveData(LiveData<ITocItem> currentKeyLiveData, boolean expanded) {
        this.currentKeyLiveData = currentKeyLiveData;
        this.expanded = expanded;
    }

    public void setExpandAll(boolean expanded) {
        this.expanded = expanded;
        executor.execute(() -> {
                expandAllInternal(UserTocLiveData.this.expanded);
                publish(false);
        });
    }

    void setExpanded(String key, boolean expanded) {
        executor.execute(new ParametrizedRunnable<Pair<String, Boolean>>() {
            @Override
            public void run(Pair<String, Boolean> parameter) {
                boolean expanded = parameter.second == null ? false : parameter.second;
                UserTocItem userTocItem = userTocMap.get(parameter.first);
                if (userTocItem != null && userTocItem.getIndexItem() != null)
                    expandInternal(userTocItem.getIndexItem(), expanded);
                publish(false);
            }
        }.set(new Pair<>(key, expanded)));
    }

    private void expandAllInternal(boolean expanded) {
        for (Map.Entry<String, UserTocItem> userTocItemEntry : userTocMap.entrySet()) {
            UserTocItem userTocItem = userTocItemEntry.getValue();
            if (userTocItem != null) {
                userTocItem.setChildsVisible(expanded);
            }
        }
    }

    private void expandInternal(ITocItem item, boolean expanded) {
        if (item instanceof Paper.Plist.Page.Article) item = ((Paper.Plist.Page.Article) item).getCategory();
        UserTocItem userTocItem = userTocMap.get(item.getKey());
        if (userTocItem != null && userTocItem.getKey()
                                              .equals(item.getKey())) {
            userTocItem.setChildsVisible(expanded);
        }
    }


    @SuppressLint("StaticFieldLeak")
    public void create(Paper.Plist plist) {
        currentKeyLiveData.observeForever(currentKeyObserver);
        executor.execute(new ParametrizedRunnable<Paper.Plist>() {
            @Override
            public void run(Paper.Plist plist) {
                Map<String, UserTocItem> result = new LinkedHashMap<>();
                for (Paper.Plist.Source source : plist.getSources()) {
                    for (Paper.Plist.Book book : source.getBooks()) {
                        for (Paper.Plist.Category category : book.getCategories()) {
                            UserTocItem first = new UserTocItem(null, category);
                            first.setChildsVisible(expanded);
                            result.put(first.getKey(), first);
                            if (category.hasIndexChilds()) {
                                for (ITocItem indexItemChild : category.getIndexChilds()) {
                                    UserTocItem child = new UserTocItem(first, indexItemChild);
                                    result.put(child.getKey(), child);
                                }
                            }
                        }
                    }
                }
                for (ITocItem toplink : plist.getToplinks()) {
                    UserTocItem item = new UserTocItem(null, toplink);
                    result.put("toplink_" + item.getKey(), item);
                }
                userTocMap.clear();
                userTocMap.putAll(result);

            }
        }.set(plist));
    }

    void setFilterBookmarks(boolean filterBookmarks) {
        if (filterBookmarks == this.filterBookmarks) return;
        this.filterBookmarks = filterBookmarks;
        publish(false);
    }

    public boolean isFilterBookmarks() {
        return filterBookmarks;
    }

    private void publish(boolean scrollToActive) {
        executor.execute(new ParametrizedRunnable<Boolean>() {
            @Override
            public void run(Boolean scrollToActive) {
                if (scrollToActive == null) scrollToActive = false;
                ResultWrapper result = new ResultWrapper();
                List<UserTocItem> shownIndex = new ArrayList<>();
                for (Map.Entry<String, UserTocItem> tocItemEntry : userTocMap.entrySet()) {
                    UserTocItem tocItem = tocItemEntry.getValue();
                    if (tocItem.isVisible() || (filterBookmarks && tocItem.getIndexItem()
                                                                          .hasBookmarkedChilds())) {
                        if (!filterBookmarks || tocItem.getIndexItem()
                                                       .isBookmarked() || tocItem.getIndexItem()
                                                                                 .hasBookmarkedChilds()) {
                            try {
                                shownIndex.add(tocItem.clone());
                                if (scrollToActive && result.scrollToPosition == -1 && tocItem.isActive()) {
                                    result.scrollToPosition = shownIndex.size() - 1;
                                    result.centerScroll = tocItem.getIndexItem() instanceof Paper.Plist.Page.Article;
                                }
                            } catch (CloneNotSupportedException e) {
                                Timber.e(e);
                            }
                        }
                    }
                }
                result.list = shownIndex;
                postValue(result);
            }
        }.set(scrollToActive));
    }

    public void onBookmarkChanged(ITocItem item) {
        executor.execute(new ParametrizedRunnable<ITocItem>() {
            @Override
            public void run(ITocItem item) {
                UserTocItem userTocItem = userTocMap.get(item.getKey());
                if (userTocItem != null) {
                    userTocItem.setBookmarkedStateFromIndex();
                    publish(false);
                }
            }
        }.set(item));
    }

    public static class ResultWrapper {
        private int scrollToPosition = -1;
        private boolean           centerScroll;
        private List<UserTocItem> list;

        int getScrollToPosition() {
            return scrollToPosition;
        }

        public List<UserTocItem> getList() {
            return list;
        }

        boolean isCenterScroll() {
            return centerScroll;
        }
    }
}
