package de.thecode.android.tazreader.reader;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

/**
 * Created by mate on 05.03.18.
 */

public class ReaderBaseFragment extends Fragment {

    public static <T extends ReaderBaseFragment> T newInstance(Class<T> clazz, String bookId) {
        T fragment = null;
        try {
            fragment = clazz.newInstance();
            Bundle args = new Bundle();
            args.putString(ReaderActivity.KEY_EXTRA_BOOK_ID, bookId);
            fragment.setArguments(args);
        } catch (java.lang.InstantiationException e) {
            Timber.e(e);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
        return fragment;
    }

    private ReaderViewModel readerViewModel;

    public ReaderBaseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String bookId = null;
        if (getArguments() != null) {
            bookId = getArguments().getString(ReaderActivity.KEY_EXTRA_BOOK_ID);
        }
        if (!TextUtils.isEmpty(bookId)) {
            readerViewModel = ViewModelProviders.of(getActivity(),
                                                    ReaderViewModel.createFactory(getActivity().getApplication(),
                                                                                  bookId))
                                                .get(ReaderViewModel.class);
        }
    }

    public ReaderViewModel getReaderViewModel() {
        return readerViewModel;
    }

    public ReaderActivity getReaderActivity() {
        return (ReaderActivity) getActivity();
    }
}
