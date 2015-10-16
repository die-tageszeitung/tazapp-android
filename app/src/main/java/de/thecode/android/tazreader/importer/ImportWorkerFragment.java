package de.thecode.android.tazreader.importer;

import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.google.common.base.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.download.UnzipPaperTask;
import de.thecode.android.tazreader.utils.AsyncTaskWithExecption;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.utils.Log;
import de.thecode.android.tazreader.utils.StorageManager;

/**
 * Created by mate on 17.04.2015.
 */
public class ImportWorkerFragment extends BaseFragment {

    private ImportRetainFragmentCallback callback;
    private List<Uri> dataUriStack;
    boolean noUserCallback;
    boolean preventImportFlag;
    boolean deleteSourceFile;
    //private Uri currentDataUri;
    private List<Paper> importedPaperStack = new ArrayList<>();

    public ImportWorkerFragment() {
    }

    public static ImportWorkerFragment findOrCreateRetainFragment(FragmentManager fm, String tag, ImportRetainFragmentCallback callback, ArrayList<Uri> dataUriStack) {
        ImportWorkerFragment fragment = (ImportWorkerFragment) fm.findFragmentByTag(tag);
        if (fragment == null) {

            fragment = new ImportWorkerFragment();
            fragment.callback = callback;
            fragment.dataUriStack = dataUriStack;

            fm.beginTransaction()
              .add(fragment, tag)
              .commit();
        } else {
            fragment.callback = callback;
        }
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (callback != null) callback.onImportRetainFragmentCreate(this);

    }


    public void handleNextDataUri() {
        Log.d();

        //if (dataUriStack != null && currentDataUri != null) dataUriStack.remove(0);
        if (dataUriStack == null || dataUriStack.size() == 0) {
            if (callback != null) callback.onFinishedImporting(importedPaperStack);
        } else {
            stepOneCopyStream(dataUriStack.get(0));
        }
    }

    private void stepOneCopyStream(Uri dataUri) {
        Log.d();

        if (dataUri.getScheme()
                   .equals(ContentResolver.SCHEME_FILE)) {
            File file = null;
            try {
                file = new File(dataUri.getPath());
                if (file.exists()) {
                    stepTwoCheckType(dataUri, file, deleteSourceFile);
                } else throw new FileNotFoundException();
            } catch (IOException e) {
                onError(dataUri, e, file, false);
            }
        } else {
            new AsyncTaskWithExecption<Object, Void, File>() {

                Uri dataUri;


                @Override
                public File doInBackgroundWithException(Object... params) throws Exception {
                    File outputFile = null;
                    try {
                        if (params.length == 2) {
                            Context context = (Context) params[0];
                            dataUri = (Uri) params[1];

                            InputStream is = context.getContentResolver()
                                                    .openInputStream(dataUri);
                            StorageManager storage = StorageManager.getInstance(context);
                            do {
                                outputFile = new File(storage.getImportCache(), String.valueOf(System.currentTimeMillis()));
                            } while (outputFile.exists());

                            OutputStream os = new FileOutputStream(outputFile);

                            final int buffer_size = 1024;
                            byte[] bytes = new byte[buffer_size];
                            for (; ; ) {
                                int count = is.read(bytes, 0, buffer_size);
                                if (count == -1) break;
                                os.write(bytes, 0, count);
                            }

                            is.close();
                            os.close();

                            return outputFile;
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } catch (Exception e) {
                        if (outputFile != null) {
                            if (outputFile.exists()) //noinspection ResultOfMethodCallIgnored
                                outputFile.delete();
                        }
                        throw e;
                    }
                }

                @Override
                protected void onPostError(Exception exception) {
                    Log.d();
                    onError(dataUri, exception, null, true);
                }

                @Override
                protected void onPostSuccess(File file) {
                    Log.d();

                    try {
                        stepTwoCheckType(dataUri, file, true);
                    } catch (IOException e) {
                        onError(dataUri, e, file, true);
                    }
                }
            }.execute(getActivity().getApplicationContext(), dataUri);
        }
    }

    private void stepTwoCheckType(Uri dataUri, File file, boolean deleteFile) throws IOException {
        Log.d();

        new AsyncTaskWithExecption<Object, Void, ImportMetadata>() {

            File file;
            Uri dataUri;
            boolean deleteFile;

            @Override
            public ImportMetadata doInBackgroundWithException(Object... params) throws Exception {
                Log.d();
                file = (File) params[0];
                dataUri = (Uri) params[1];
                deleteFile = (boolean) params[2];
                return ImportMetadata.parse(file);
            }

            @Override
            protected void onPostError(Exception exception) {
                Log.d();

                onError(dataUri, exception, file, deleteFile);
            }

            @Override
            protected void onPostSuccess(ImportMetadata importMetadata) {
                stepThreeCheckDatabase(dataUri, importMetadata, file, deleteFile);
            }
        }.execute(file, dataUri, deleteFile);
    }

    private void stepThreeCheckDatabase(Uri dataUri, ImportMetadata data, File file, boolean deleteFile) {
        Log.d();
        if (data != null) {
            try {
                Paper paper = new Paper(getActivity().getApplicationContext(), data.getBookId());
                if (paper.isDownloaded()) {

                    if (noUserCallback) throw new Paper.PaperNotFoundException();
                    else {
                        if (callback != null) callback.onImportAlreadyExists(dataUri, data, file, deleteFile);
                        else {
                            onFinished(dataUri, file, deleteFile);
                        }
                    }
                } else {
                    throw new Paper.PaperNotFoundException();
                }

            } catch (Paper.PaperNotFoundException e) {
                stepFourStartImport(dataUri, data, file, deleteFile);
            }

        }
    }

    public void stepFourStartImport(Uri dataUri, ImportMetadata data, File file, boolean deleteFile) {
        Log.d();
        try {
            switch (data.getType()) {
                case TAZANDROID:
                    importTazAndroid(dataUri, data, file, deleteFile);
                    break;
                case TPAPER:
                    importTpaper(dataUri, data, file, deleteFile);
                    break;
            }

        } catch (ImportException | FileNotFoundException e) {
            onError(dataUri, e, file, deleteFile);
        }
    }


    public void onFinished(Uri dataUri, File cacheFile, boolean deleteFile) {
        remove(dataUri, cacheFile, deleteFile);
        handleNextDataUri();
    }

    public void remove(Uri dataUri, File cacheFile, boolean deleteFile) {
        dataUriStack.remove(dataUri);
        if (deleteFile) if (cacheFile != null) if (cacheFile.exists()) //noinspection ResultOfMethodCallIgnored
            cacheFile.delete();
    }

    public void onError(Uri dataUri, Exception e, File cacheFile, boolean deleteFile) {
        if (noUserCallback) onFinished(dataUri, cacheFile, deleteFile);
        else if (callback != null) {
            remove(dataUri, cacheFile, deleteFile);
            callback.onErrorWhileImport(dataUri, e);
        }
    }


    private void importTazAndroid(Uri dataUri, ImportMetadata metadata, File cacheFile, boolean deleteFile) throws ImportException {

        StorageManager storage = StorageManager.getInstance(getActivity());

        Paper paper;
        try {
            paper = new Paper(getActivity().getApplicationContext(), metadata.getBookId());
            storage.deletePaperDir(paper);
        } catch (Paper.PaperNotFoundException e) {
            paper = new Paper();
            paper.setBookId(metadata.getBookId());
            paper.setDate(metadata.getDate());

        }
        if (!Strings.isNullOrEmpty(metadata.getArchive())) {
            paper.setLink(Uri.parse(getString(R.string.archiveUrl))
                             .buildUpon()
                             .appendEncodedPath(metadata.getArchive())
                             .build()
                             .toString());
            paper.setHasupdate(true);
        }
        paper.setImage(null);
        paper.setImageHash(null);
        if (!preventImportFlag) paper.setImported(true);
        paper.setDownloaded(false);
        paper.setFileHash(null);
        paper.setResourceUrl(null);
        paper.setResourceFileHash(null);
        paper.setResource(null);
        if (paper.getId() != null) {
            int affected = getActivity().getApplicationContext()
                                        .getContentResolver()
                                        .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);
            if (affected == 0) {
                throw new ImportException("Could not update " + metadata.getBookId());
            }
        } else {
            long newId = ContentUris.parseId(getActivity().getApplicationContext()
                                                          .getContentResolver()
                                                          .insert(Paper.CONTENT_URI, paper.getContentValues()));
            if (newId == -1) throw new ImportException("Could not insert " + metadata.getBookId());
            else paper.setId(newId);
        }
        importedPaperStack.add(paper);
        onFinished(dataUri, cacheFile, deleteFile);
    }

    private void importTpaper(Uri dataUri, ImportMetadata metadata, File cacheFile, boolean deleteFile) throws FileNotFoundException {
        StorageManager storage = StorageManager.getInstance(getActivity());

        Paper paper;

        try {
            paper = new Paper(getActivity().getApplicationContext(), metadata.getBookId());
            storage.deletePaperDir(paper);
        } catch (Paper.PaperNotFoundException e) {
            paper = new Paper();
            paper.setBookId(metadata.getBookId());
            paper.setDate(metadata.getDate());

        }
        paper.setImage(null);
        paper.setImageHash(null);
        if (!preventImportFlag) paper.setImported(true);
        paper.setDownloaded(true);
        paper.setFileHash(null);
        paper.setResourceUrl(null);
        paper.setResourceFileHash(null);
        paper.setResource(null);


        new UnzipPaperTask(paper, cacheFile, storage.getPaperDirectory(paper), deleteFile) {
            Context context;
            Uri dataUri;
            File cacheFile;
            boolean deleteFile;

            @Override
            public File doInBackgroundWithException(Object... params) throws Exception {
                context = (Context) params[0];
                dataUri = (Uri) params[1];
                cacheFile = (File) params[2];
                deleteFile = (boolean) params[3];
                return super.doInBackgroundWithException(params);
            }

            @Override
            public void onPostError(Exception exception, File sourceZipFile) {
                onError(dataUri, exception, cacheFile, deleteFile);
            }

            @Override
            protected void onPostSuccess(File destinationFile) {
                super.onPostSuccess(destinationFile);
                try {
                    Paper paper = getUnzipPaper().getPaper();
                    paper.parseMissingAttributes(false);
                    if (paper.getId() != null) {
                        int affected = getActivity().getApplicationContext()
                                                    .getContentResolver()
                                                    .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);
                        if (affected == 0) {
                            throw new ImportException("Could not update " + paper.getBookId());
                        }
                    } else {
                        long newId = ContentUris.parseId(getActivity().getApplicationContext()
                                                                      .getContentResolver()
                                                                      .insert(Paper.CONTENT_URI, paper.getContentValues()));
                        if (newId == -1) throw new ImportException("Could not insert " + paper.getBookId());
                        else paper.setId(newId);
                    }
                    importedPaperStack.add(paper);
                    onFinished(dataUri, cacheFile, deleteFile);
                } catch (ImportException e) {
                    onError(dataUri, e, cacheFile, deleteFile);
                }
            }
        }.execute(getActivity().getApplicationContext(), dataUri, cacheFile, deleteFile);
    }


    public interface ImportRetainFragmentCallback {
        public void onImportRetainFragmentCreate(ImportWorkerFragment importRetainWorkerFragment);
        public void onFinishedImporting(List<Paper> papersToDownload);
        public void onErrorWhileImport(Uri dataUri, Exception e);
        public void onImportAlreadyExists(Uri dataUri, ImportMetadata metadata, File cacheFile, boolean deleteFile);
    }


    public static class ImportException extends Exception {
        public ImportException() {
        }

        public ImportException(String detailMessage) {
            super(detailMessage);
        }

        public ImportException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public ImportException(Throwable throwable) {
            super(throwable);
        }
    }


}
