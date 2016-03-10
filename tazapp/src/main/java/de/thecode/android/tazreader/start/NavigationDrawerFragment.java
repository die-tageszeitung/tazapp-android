package de.thecode.android.tazreader.start;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationDrawerFragment extends BaseFragment {

    private static final Logger log = LoggerFactory.getLogger(NavigationDrawerFragment.class);

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

    WeakReference<IStartCallback> startCallback;

    int mActive = -1;

    private List<Item> items = new ArrayList<>();

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.trace("");

        startCallback = new WeakReference<>((IStartCallback) getActivity());

        if (savedInstanceState != null) mActive = savedInstanceState.getInt(KEY_ACTIVE);


        mUserLearnedDrawer = TazSettings.getPrefBoolean(getActivity(), TazSettings.PREFKEY.NAVDRAWERLEARNED, false);
        if (savedInstanceState != null) mFromSavedInstanceState = true;
        mClickListener = new Item.ClickListener() {
            @Override
            public void onItemClick(int position) {
                //Give the User a chance to see touchfeedback, therefor its delayed
                mDrawerLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.closeDrawer(mDrawerContainerView);
                    }
                }, CLOSE_DRAWER_DELAY);
                if (mActive != position) {
                    onClick(position);
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log.trace("");
        View view = inflater.inflate(R.layout.start_navigation, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    private boolean hasCallback() {
        return startCallback.get() != null;
    }

    private IStartCallback getCallback() {
        return startCallback.get();
    }


    public void setUp(int drawerId, DrawerLayout drawerLayout, Toolbar toolbar) {
        log.trace("");
        mDrawerContainerView = getActivity().findViewById(drawerId);
        mDrawerLayout = drawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    TazSettings.setPref(getActivity(), TazSettings.PREFKEY.NAVDRAWERLEARNED, true);
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
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        navigationAdapter = new NavigationAdapter();
        //
        //        UserItem userItem = new UserItem();
        //        userItem.setUsername(getActivity());
        //        navigationAdapter.addItem(userItem);
        //        navigationAdapter.addItem(new Item(Item.TYPE.DIVIDER));
        //        navigationAdapter.addItem(new NavigationItem(getString(R.string.drawer_library),R.drawable.ic_library,StartActivity.FragmentFactory.LIBRARY_FRAGMENT));

        mRecyclerView.setAdapter(navigationAdapter);
        addItem(new Item(Item.TYPE.HEADER));

    }

    public void setEnabled(boolean bool) {
        if (!bool) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            if (hasCallback()) getCallback().getToolbar()
                                            .setVisibility(View.INVISIBLE);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (hasCallback()) getCallback().getToolbar()
                                            .setVisibility(View.VISIBLE);
        }
    }

    public void addItem(Item item) {
        if (items.contains(item)) {
            log.error("Ignored adding item to Navihgation drawer, can only be added once");
            return;
        }
        item.position = items.size();
        items.add(item);
        mRecyclerView.getAdapter()
                     .notifyItemInserted(item.getPosition());
    }

    public void handleChangedItem(Item item) {
        navigationAdapter.notifyItemChanged(item.getPosition());
    }

    public void setActive(Item item) {
        navigationAdapter.setActive(item.getPosition());
    }

    public void setActive(Class<? extends Fragment> fragmentClass) {
        for (Item item : items) {
            if (item instanceof NavigationItem) {
                if (fragmentClass == ((NavigationItem) item).getTarget()) {
                    navigationAdapter.setActive(item.getPosition());
                    return;
                }
            }
        }
    }


    public void addDividerItem() {
        addItem(new Item(Item.TYPE.DIVIDER));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTIVE, mActive);
    }

    public void onClick(int position) {
        log.debug("position: {}",position);
        //navigationAdapter.setActive(position);
        Item item = navigationAdapter.getItem(position);
        if (item instanceof NavigationItem) {
            if (hasCallback()) getCallback().loadFragment((NavigationItem) item);
        }
    }

    public void simulateClick(NavigationItem item, boolean closeDrawer) {
        if (closeDrawer) mDrawerLayout.closeDrawer(mDrawerContainerView);
        onClick(item.getPosition());
    }


    private class NavigationAdapter extends RecyclerView.Adapter<ViewHolder> {


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            log.trace("");
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
        public void onBindViewHolder(ViewHolder holder, int position) {
            log.trace("");

            switch (items.get(position)
                         .getType()) {

                case ENTRY:
                    //                case USER:
                    NavigationItemViewHolder itemViewHolder = (NavigationItemViewHolder) holder;
                    NavigationItem item = (NavigationItem) items.get(position);
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

        //        private void addItem(Item item) {
        //            items.add(item);
        //        }

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

        ;

        //        public int getPositionOfItemWithFracmentFactoryId(int fragmentFactoryId) {
        //            for (Item item : items) {
        //                if (item instanceof NavigationItem) {
        //                    if (((NavigationItem) item).getFragmentFactoryId() == fragmentFactoryId)
        //                        return items.indexOf(item);
        //                }
        //            }
        //            return -1;
        //        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Item.ClickListener clickListener;

        public ViewHolder(View itemView) {
            super(itemView);
        }


        public ViewHolder(View itemView, Item.ClickListener clickListener) {
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

        public NavigationItemViewHolder(View itemView, Item.ClickListener clickListener) {
            super(itemView, clickListener);
            layout = (RelativeLayout) itemView.findViewById(R.id.layout);
            text = (TextView) itemView.findViewById(R.id.text);
            image = (ImageView) itemView.findViewById(R.id.image);

        }

        @Override
        public void setActive() {
            image.setColorFilter(itemView.getContext()
                                         .getResources()
                                         .getColor(R.color.index_bookmark_on));
        }

        @Override
        public void setInactive() {
            image.setColorFilter(itemView.getContext()
                                         .getResources()
                                         .getColor(R.color.index_bookmark_off));
        }

    }

    private static class HeaderViewHolder extends ViewHolder {
        RelativeLayout layout;
        TextView text;

        public HeaderViewHolder(View itemView) {
            super(itemView, null);
            layout = (RelativeLayout) itemView.findViewById(R.id.layout);
            ViewCompat.setImportantForAccessibility(layout, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            text = (TextView) itemView.findViewById(R.id.text);
            text.setVisibility(View.VISIBLE);
            if (BuildConfig.VERSION_NAME
                   .toLowerCase()
                   .contains("alpha")) text.setText(R.string.drawer_header_alpha);
            else if (BuildConfig.VERSION_NAME
                        .toLowerCase()
                        .contains("beta")) text.setText(R.string.drawer_header_beta);
            else if (BuildConfig.VERSION_NAME
                        .toLowerCase()
                        .contains("debug")) text.setText(R.string.drawer_header_debug);
            else text.setVisibility(View.GONE);
        }
    }

    public static class Item {
        enum TYPE {HEADER, ENTRY, DIVIDER}

        TYPE type;
        private int position;
        private Class<? extends Fragment> target;

        public Item(TYPE type) {
            this.type = type;
        }

        public TYPE getType() {
            return type;
        }

        public interface ClickListener {
            public void onItemClick(int position);
        }

        public int getPosition() {
            return position;
        }

        public void setTarget(Class<? extends Fragment> target) {
            this.target = target;
        }

        public Class<? extends Fragment> getTarget() {
            return target;
        }
    }

    public static class NavigationItem extends Item {

        String text;
        int drawableId;
        boolean accessibility = true;

        //        int fragmentFactoryId; //From FragmentFactory

        public NavigationItem(String text, Class<? extends Fragment> target) {
            this(text, -1, target);
        }

        public NavigationItem(String text, int drawableId, Class<? extends Fragment> target) {
            this(TYPE.ENTRY, text, drawableId, target);
        }

        public NavigationItem(TYPE type, String text, int drawableId, Class<? extends Fragment> target) {
            super(type);
            this.text = text;
            setTarget(target);
            //            this.fragmentFactoryId = fragmentFactoryId;
            if (drawableId == -1) this.drawableId = R.drawable.ic_dot;
            else this.drawableId = drawableId;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setAccessibilty(boolean bool) {
            accessibility = bool;
        }
    }
}
