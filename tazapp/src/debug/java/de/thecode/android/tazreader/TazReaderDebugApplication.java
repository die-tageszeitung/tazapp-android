package de.thecode.android.tazreader;

import com.facebook.stetho.Stetho;

/**
 * Created by mate on 30.03.2015.
 */
public class TazReaderDebugApplication extends TazReaderApplication {
    @Override
    public void onCreate() {

        super.onCreate();
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                                .build());
    }

//    @Override
//    public RefWatcher installLeakCanary() {
//        return LeakCanary.install(this);
//    }
}
