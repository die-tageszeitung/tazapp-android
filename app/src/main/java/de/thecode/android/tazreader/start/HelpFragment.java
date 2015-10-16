package de.thecode.android.tazreader.start;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.viewpagerindicator.CirclePageIndicator;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class HelpFragment extends BaseFragment {
    private IStartCallback callback;
    private ViewPager viewPager;

    public HelpFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getActivity() instanceof IStartCallback) {
            callback = (IStartCallback) getActivity();
            callback.onUpdateDrawer(this);
        }
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_help, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.viewpager);
        ImagePagerAdapter adapter = new ImagePagerAdapter();
        viewPager.setAdapter(adapter);
        CirclePageIndicator indicator = (CirclePageIndicator) view.findViewById(R.id.indicator);
        indicator.setViewPager(viewPager);
        return view;
    }


    private class ImagePagerAdapter extends PagerAdapter {

        private int[] mImages = new int[]{R.drawable.help_library, R.drawable.help_page, R.drawable.help_article, R.drawable.help_index};

        @Override
        public int getCount() {
            return mImages.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((ImageView) object);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            ImageView imageView = new ImageView(getActivity());
            int padding = getResources().getDimensionPixelSize(R.dimen.help_padding);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(mImages[position]);
            container.addView(imageView, 0);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((ImageView) object);
        }
    }


}
