package de.thecode.android.tazreader.utils;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;


public class PlistHelper {

    public static String getString(NSDictionary dict, String key) {
        if (dict.containsKey(key))
            return getString(dict.get(key));
        return null;
    }

    public static Integer getInt(NSDictionary dict, String key) {
        if (dict != null && dict.containsKey(key))
            return getInt(dict.get(key));
        return null;
    }

    public static boolean getBoolean(NSDictionary dict, String key) {
        return dict.containsKey(key) && getBoolean(dict.get(key));
    }

    public static Long getLong(NSDictionary dict, String key) {
        if (dict.containsKey(key))
            return getLong(dict.get(key));
        return null;
    }

    public static Float getFloat(NSDictionary dict, String key) {
        if (dict.containsKey(key))
            return getFloat(dict.get(key));
        return null;
    }

    public static String getString(NSObject object) {
        if (checkClass(object, NSString.class)) {
            return ((NSString) object).getContent();
        }
        return null;
    }


    public static Integer getInt(NSObject object) {
        NSNumber temp = getNumber(object);
        if (temp != null) {
            if (temp.isInteger())
                return temp.intValue();
        }
        return null;
    }

    public static boolean getBoolean(NSObject object) {
        NSNumber temp = getNumber(object);
        if (temp != null) {
            if (temp.isBoolean())
                return temp.boolValue();
        }
        return false;
    }

    public static Long getLong(NSObject object) {
        NSNumber temp = getNumber(object);
        if (temp != null) {
            if (temp.isReal())
                return temp.longValue();
        }
        return null;
    }

    public static Float getFloat(NSObject object) {
        NSNumber temp = getNumber(object);
        if (temp != null) {
            if (temp.isReal())
                return temp.floatValue();
            else
                return (float)temp.intValue();
        }
        return null;
    }

    //----

    private static NSNumber getNumber(NSObject object) {
        if (checkClass(object, NSNumber.class)) {
            return (NSNumber) object;
        }
        return null;
    }


    private static boolean checkClass(NSObject object, @SuppressWarnings("rawtypes") Class cls) {
        return object.getClass()
                     .equals(cls);
    }


}
