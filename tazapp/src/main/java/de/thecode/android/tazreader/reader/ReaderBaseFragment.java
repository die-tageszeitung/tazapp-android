package de.thecode.android.tazreader.reader;

import android.os.Bundle;
import android.text.TextUtils;

import de.thecode.android.tazreader.utils.BaseFragment;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import timber.log.Timber;

/**
 * Created by mate on 05.03.18.
 */

public class ReaderBaseFragment extends BaseFragment {

    public static <T extends ReaderBaseFragment> T newInstance(Class<T> clazz, String bookId) {
        T fragment = null;
        try {
            fragment = clazz.newInstance();
            Bundle args = new Bundle();
            args.putString(ReaderActivity.KEY_EXTRA_BOOK_ID, bookId);
//            args.putString(ReaderActivity.KEY_EXTRA_RESOURCE_KEY, resourceKey);
            fragment.setArguments(args);
        } catch (java.lang.InstantiationException e) {
            Timber.e(e);
        } catch (IllegalAccessException e) {
            Timber.e(e);
        }
        return fragment;
    }

    private ReaderViewModel readerViewModel;
    private ReaderTTSViewModel ttsViewModel;
    private ReaderAudioViewModel audioViewModel;

    public ReaderBaseFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String bookId = null;
//        String resourceKey = null;
        if (getArguments() != null) {
            bookId = getArguments().getString(ReaderActivity.KEY_EXTRA_BOOK_ID);
//            resourceKey = getArguments().getString(ReaderActivity.KEY_EXTRA_RESOURCE_KEY);
        }
        if (!TextUtils.isEmpty(bookId)) {
            readerViewModel = ViewModelProviders.of(getActivity(),
                                                    ReaderViewModel.createFactory(getActivity().getApplication(),
                                                                                  bookId))
                                                .get(ReaderViewModel.class);
        }
        ttsViewModel = ViewModelProviders.of(getActivity())
                                         .get(ReaderTTSViewModel.class);
        audioViewModel = ViewModelProviders.of(getActivity())
                                         .get(ReaderAudioViewModel.class);

    }

    public ReaderViewModel getReaderViewModel() {
        return readerViewModel;
    }

    public ReaderTTSViewModel getTtsViewModel() {
        return ttsViewModel;
    }

    public ReaderAudioViewModel getAudioViewModel() {
        return audioViewModel;
    }

    public ReaderActivity getReaderActivity() {
        return (ReaderActivity) getActivity();
    }
}
