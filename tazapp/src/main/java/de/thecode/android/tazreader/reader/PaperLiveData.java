package de.thecode.android.tazreader.reader;

import android.annotation.SuppressLint;
import androidx.lifecycle.LiveData;
import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.utils.AsyncTaskWithException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by mate on 02.03.18.
 */

public class PaperLiveData extends LiveData<Paper> {

    private final String          bookId;
    private final PaperRepository paperRepository;
    private final StorageManager  storageManager;
    private final StoreRepository storeRepository;
    private final ResourceRepository resourceRepository;
    private Resource resource;

    PaperLiveData(Context context, String bookId) {
        this.bookId = bookId;
        paperRepository = PaperRepository.getInstance(context);
        storageManager = StorageManager.getInstance(context);
        storeRepository = StoreRepository.getInstance(context);
        resourceRepository = ResourceRepository.getInstance(context);
        loadData();
    }

    @SuppressLint("StaticFieldLeak")
    private void loadData() {
        new AsyncTaskWithException<Void, Void, Paper>() {

            @Override
            public Paper doInBackgroundWithException(Void... voids) throws Exception {
                Paper paper = paperRepository.getPaperWithBookId(bookId);
                if (paper == null) throw new Paper.PaperNotFoundException();
                resource = resourceRepository.getResourceForPaper(paper);
                paper.parsePlist(new File(storageManager.getPaperDirectory(paper), Paper.CONTENT_PLIST_FILENAME));
                String bookmarkJsonString = storeRepository.getStore(bookId,Paper.STORE_KEY_BOOKMARKS)
                                                           .getValue();
                if (!TextUtils.isEmpty(bookmarkJsonString)) {
                    JSONArray bookmarksJsonArray = new JSONArray(bookmarkJsonString);
                    for (int i = 0; i < bookmarksJsonArray.length(); i++) {
                        ITocItem item = paper.getPlist()
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
                                            i.remove();
                                        }
                                    }
                                }
                                for (Paper.Plist.Page.Article article : page.getArticles()) {
                                    if (!article.isLink()) {
                                        articleCollectionOrder.put(article.getKey(), position);
                                        articleCollectionPositionIndex.put(position, article.getKey());
                                        position++;
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

                paper.setArticleCollectionOrder(articleCollectionOrder);
                paper.setArticleCollectionPositionIndex(articleCollectionPositionIndex);

                return paper;

            }

            @Override
            protected void onPostError(Exception exception) {
                Timber.e(exception);
            }

            @Override
            protected void onPostSuccess(Paper paper) {
                setValue(paper);
            }
        }.execute();
    }

    public Resource getResource() {
        return resource;
    }
}
