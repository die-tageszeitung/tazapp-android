package de.thecode.android.tazreader.reader;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;
import de.thecode.android.tazreader.utils.StorageManager;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by mate on 26.04.2017.
 */

public abstract class PaperLoadingTask extends AsyncTaskWithExecption<Void, Void, Paper> {

    private final Context mContext;
    private final long    mPaperId;

    public PaperLoadingTask(Context context, long paperId) {
        this.mContext = context;
        this.mPaperId = paperId;
    }

    @Override
    public Paper doInBackgroundWithException(Void... params) throws Exception {
        Paper paper = Paper.getPaperWithId(mContext, mPaperId);
        if (paper == null) throw new Paper.PaperNotFoundException();
        //paper.parsePlist(mStorage.getPaperFile(paper));
        paper.parsePlist(new File(StorageManager.getInstance(mContext)
                                                .getPaperDirectory(paper), Paper.CONTENT_PLIST_FILENAME));
        String bookmarkJsonString = StoreRepository.getInstance(mContext)
                                                   .getStoreForKey(paper.getStorePath(Paper.STORE_KEY_BOOKMARKS))
                                                   .getValue();
        if (!TextUtils.isEmpty(bookmarkJsonString)) {
            JSONArray bookmarksJsonArray = new JSONArray(bookmarkJsonString);
            for (int i = 0; i < bookmarksJsonArray.length(); i++) {
                IIndexItem item = paper.getPlist()
                                       .getIndexItem(bookmarksJsonArray.getString(i));
                if (item != null) {
                    item.setBookmark(true);
                }
            }
        }


        Map<String, Integer> articleCollectionOrder = new HashMap<>();
        Map<Integer, String> articleCollectionPositionIndex = new HashMap<>();

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

        paper.setArticleCollectionOrder(articleCollectionOrder);
        paper.setArticleCollectionPositionIndex(articleCollectionPositionIndex);

        return paper;
    }
}
