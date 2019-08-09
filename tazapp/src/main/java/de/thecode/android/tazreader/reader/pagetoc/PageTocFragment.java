package de.thecode.android.tazreader.reader.pagetoc;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.reader.ReaderBaseFragment;
import de.thecode.android.tazreader.widget.SnapLayoutManager;

import timber.log.Timber;

public class PageTocFragment extends ReaderBaseFragment {

    PageTocAdapter adapter;

    RecyclerView mRecyclerView;

    public PageTocFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new PageTocAdapter(PageTocFragment.this::onItemClick);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.reader_pageindex, container, false);
        mRecyclerView = view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        SnapLayoutManager layoutManager = new SnapLayoutManager(inflater.getContext());
        layoutManager.setSnapPreference(SnapLayoutManager.SNAP_TO_CENTER);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getReaderViewModel().getPageTocLiveData().observe(this, resultWrapper -> {
                if (resultWrapper != null) {
                    adapter.submitList(resultWrapper.getList());
                    if (resultWrapper.getScrollToPosition() != -1) {
                        mRecyclerView.smoothScrollToPosition(resultWrapper.getScrollToPosition());
                    }
                }
        });
    }

    private void onItemClick(int position) {
        Timber.d("position: %s", position);
        PageTocItem item = adapter.getItem(position);
        getReaderActivity().loadContentFragment(item.getKey());
        getReaderActivity().closeDrawers();
    }


}
