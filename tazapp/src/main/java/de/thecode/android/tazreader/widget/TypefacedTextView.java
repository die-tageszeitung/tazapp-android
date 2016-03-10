package de.thecode.android.tazreader.widget;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import de.thecode.android.tazreader.R;

public class TypefacedTextView extends TextView {

    private static final Logger log = LoggerFactory.getLogger(TypefacedTextView.class);

    private static Map<String,Typeface> typefacemap = new HashMap<>();

	public TypefacedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Typeface.createFromAsset doesn't work in the layout editor. Skipping...
        if (isInEditMode()) {
            return;
        }

        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.TypefacedTextView);
        String fontName = styledAttrs.getString(R.styleable.TypefacedTextView_typeface);
        styledAttrs.recycle();

        if (fontName != null) {
            //Typeface typeface = Typeface.createFromAsset(context.getAssets(), fontName);
            setTypeface(createTypeface(context,fontName));
        }
    }

    private static Typeface createTypeface(Context context, String name) {
        if (typefacemap.containsKey(name)) {
            return typefacemap.get(name);
        }
        else
        {
            log.debug("Did not find typeface, create {}", name);
            Typeface typeface = Typeface.createFromAsset(context.getAssets(), name);
            typefacemap.put(name, typeface);
            return typeface;
        }
    }
}
