package de.thecode.android.tazreader.start;


import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationDrawerFragment extends StartBaseFragment {

    private static final String KEY_ACTIVE = "active";
    private static final int CLOSE_DRAWER_DELAY = 300;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;

    private boolean mUserLearnedDrawer;
    private boolean mFromSavedInstanceState;
    private View mDrawerContainerView;
    private RecyclerView mRecyclerView;

    private Item.ClickListener mClickListener;
    private NavigationAdapter navigationAdapter;

    private int mActive = -1;

    private List<Item> items = new ArrayList<>();

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) mActive = savedInstanceState.getInt(KEY_ACTIVE);


        mUserLearnedDrawer = TazSettings.getInstance(getActivity()).getPrefBoolean(TazSettings.PREFKEY.NAVDRAWERLEARNED, false);
        if (savedInstanceState != null) mFromSavedInstanceState = true;
        mClickListener = position -> {
                //Give the User a chance to see touchfeedback, therefor its delayed
                mDrawerLayout.postDelayed(() -> mDrawerLayout.closeDrawer(mDrawerContainerView),
                        CLOSE_DRAWER_DELAY);
                if (mActive != position) {
                    onClick(position);
                }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.start_navigation, container, false);
        mRecyclerView = view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    public void setUp(int drawerId, DrawerLayout drawerLayout, Toolbar toolbar) {

        Activity activity = getActivity();
        if (activity != null) {
            mDrawerContainerView = getActivity().findViewById(drawerId);
        }

        mDrawerLayout = drawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    TazSettings.getInstance(getActivity()).setPref(TazSettings.PREFKEY.NAVDRAWERLEARNED, true);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                EventBus.getDefault()
                        .post(new DrawerStateChangedEvent(newState));
            }
        };
        if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
            mDrawerLayout.openDrawer(mDrawerContainerView);
        }
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerLayout.post(() -> mDrawerToggle.syncState());

        navigationAdapter = new NavigationAdapter();

        mRecyclerView.setAdapter(navigationAdapter);
        addItem(new Item(Item.TYPE.HEADER));

    }

    void setEnabled(boolean bool) {
        if (!bool) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            getStartActivity().getToolbar()
                                            .setVisibility(View.INVISIBLE);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            getStartActivity().getToolbar()
                                            .setVisibility(View.VISIBLE);
        }
    }

    void addItem(Item item) {
        if (items.contains(item)) {
            Timber.e("Ignored adding item to Navihgation drawer, can only be added once");
            return;
        }
        item.position = items.size();
        items.add(item);
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter != null)
            adapter.notifyItemInserted(item.getPosition());
    }

    void setActive(Class<? extends Fragment> fragmentClass) {
        for (Item item : items) {
            if (item instanceof NavigationItem) {
                if (fragmentClass == ((NavigationItem) item).getTarget()) {
                    navigationAdapter.setActive(item.getPosition());
                    return;
                }
            }
        }
    }


    void addDividerItem() {
        addItem(new Item(Item.TYPE.DIVIDER));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTIVE, mActive);
    }

    private void onClick(int position) {
        Timber.d("position: %s",position);
        //navigationAdapter.setActive(position);
        Item item = navigationAdapter.getItem(position);
        if (item instanceof NavigationItem) {
            getStartActivity().loadFragment((NavigationItem) item);
        } else if (item instanceof ClickItem) {
            getStartActivity().onNavigationClick((ClickItem) item);
        }
    }

    void simulateClick(NavigationItem item, boolean closeDrawer) {
        if (closeDrawer) mDrawerLayout.closeDrawer(mDrawerContainerView);
        onClick(item.getPosition());
    }


    private class NavigationAdapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            switch (Item.TYPE.values()[viewType]) {
                case HEADER:
                    return new HeaderViewHolder(LayoutInflater.from(parent.getContext())
                                                              .inflate(R.layout.start_navigation_header, parent, false));
                case ENTRY:
                    //                case USER:
                    return new NavigationItemViewHolder(LayoutInflater.from(parent.getContext())
                                                                      .inflate(R.layout.start_navigation_item, parent, false), mClickListener);
                case DIVIDER:
                    return new ViewHolder(LayoutInflater.from(parent.getContext())
                                                        .inflate(R.layout.start_navigation_divider, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            switch (items.get(position)
                         .getType()) {

                case ENTRY:
                    //                case USER:
                    NavigationItemViewHolder itemViewHolder = (NavigationItemViewHolder) holder;
                    ClickItem item = (ClickItem) items.get(position);
                    itemViewHolder.image.setImageResource(item.drawableId);
                    itemViewHolder.text.setText(item.text);

                    break;
            }

            if (mActive == position) holder.setActive();
            else holder.setInactive();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position)
                        .getType()
                        .ordinal();
        }

        private void setActive(int postion) {
            int oldActive = mActive;
            mActive = postion;
            notifyItemChanged(oldActive);
            notifyItemChanged(mActive);
        }

        public Item getItem(int position) {
            try {
                return items.get(position);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Item.ClickListener clickListener;

        ViewHolder(View itemView) {
            super(itemView);
        }


        ViewHolder(View itemView, Item.ClickListener clickListener) {
            super(itemView);
            this.clickListener = clickListener;
            if (clickListener != null) {
                itemView.setOnClickListener(this);
                itemView.setClickable(true);
            }
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) clickListener.onItemClick(getPosition());
        }

        public void setActive() {
        }

        public void setInactive() {
        }
    }

    private static class NavigationItemViewHolder extends ViewHolder {

        TextView text;
        ImageView image;
        RelativeLayout layout;

        NavigationItemViewHolder(View itemView, Item.ClickListener clickListener) {
            super(itemView, clickListener);
            layout = itemView.findViewById(R.id.layout);
            text = itemView.findViewById(R.id.text);
            image = itemView.findViewById(R.id.image);

        }

        @Override
        public void setActive() {
            image.setColorFilter(itemView.getContext()
                                         .getResources()
                                         .getColor(R.color.start_navigation_item_icon_active));
        }

        @Override
        public void setInactive() {
            image.setColorFilter(itemView.getContext()
                                         .getResources()
                                         .getColor(R.color.start_navigation_item_icon));
        }

    }

    private static class HeaderViewHolder extends ViewHolder {
        RelativeLayout layout;

        HeaderViewHolder(View itemView) {
            super(itemView, null);
            layout = itemView.findViewById(R.id.layout);
            ViewCompat.setImportantForAccessibility(layout, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
    }

    public static class Item {
        enum TYPE {HEADER, ENTRY, DIVIDER}

        TYPE type;
        private int position;


        Item(TYPE type) {
            this.type = type;
        }

        public TYPE getType() {
            return type;
        }

        public interface ClickListener {
            void onItemClick(int position);
        }

        public int getPosition() {
            return position;
        }


    }

    public static class ClickItem extends Item {

        String text;
        int drawableId;
        boolean accessibility = true;

        public ClickItem(String text) {
            this(text,-1);
        }

        ClickItem(String text, int drawableId) {
            this(TYPE.ENTRY,text,drawableId);
        }

        private ClickItem(TYPE type, String text, int drawableId) {
            super(type);
            this.text = text;
            if (drawableId == -1) this.drawableId = R.drawable.ic_dot;
            else this.drawableId = drawableId;
        }

        public void setText(String text) {
            this.text = text;
        }

        void setAccessibilty(boolean bool) {
            accessibility = bool;
        }
    }

    public static class NavigationItem extends ClickItem {

        private Class<? extends Fragment> target;

        public NavigationItem(String text, Class<? extends Fragment> target) {
            this(text, -1, target);
        }

        NavigationItem(String text, int drawableId, Class<? extends Fragment> target) {
            this(TYPE.ENTRY, text, drawableId, target);
        }

        private NavigationItem(TYPE type, String text, int drawableId, Class<? extends Fragment> target) {
            super(type,text,drawableId);
            setTarget(target);
        }

        void setTarget(Class<? extends Fragment> target) {
            this.target = target;
        }

        Class<? extends Fragment> getTarget() {
            return target;
        }

    }
}
