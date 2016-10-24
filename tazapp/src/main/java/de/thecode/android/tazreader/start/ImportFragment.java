package de.thecode.android.tazreader.start;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.ref.WeakReference;

import de.mateware.dialog.Dialog;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.importer.ImportActivity;
import de.thecode.android.tazreader.utils.BaseFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImportFragment extends BaseFragment implements ImportDataRetainFragment.ImportDataCallback {


    private static final Logger log = LoggerFactory.getLogger(ImportFragment.class);

    public static final String DIALOG_PERMISSION_WRITE = "DialogPermissionWrite";
    private static int PERMISSION_REQUEST_IMPORT_WRITE = 345;

    private static String ARG_IS_SHOWING_EXPLENATION_DIALOG = "argIsShowingED";
    private static String ARG_IS_REQUESTING_PERMISSION = "argIsReqPerm";

    ImportRecyclerAdapter adapter;

    private WeakReference<IStartCallback> callback;
    private ImportDataRetainFragment dataFragment;
    private boolean isShowingExplenationDialog;
    private boolean isRequestinPermission;

    public ImportFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        log.debug("");
        callback = new WeakReference<>((IStartCallback) getActivity());
        if (hasCallback()) getCallback().onUpdateDrawer(this);
        dataFragment = ImportDataRetainFragment.findOrCreateRetainFragment(getFragmentManager(), this);
        isShowingExplenationDialog = savedInstanceState != null && savedInstanceState.getBoolean(ARG_IS_SHOWING_EXPLENATION_DIALOG, false);
        isRequestinPermission = savedInstanceState != null && savedInstanceState.getBoolean(ARG_IS_REQUESTING_PERMISSION, false);
        if (!isShowingExplenationDialog && !isRequestinPermission) startWithPermissionCheck();
    }

    public void startWithPermissionCheck() {
        isShowingExplenationDialog = false;
        isRequestinPermission = false;
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            isRequestinPermission = true;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_IMPORT_WRITE);
            //ActivityCompat.requestPermissions(getActivity(), );

            // Should we show an explanation?
            //            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            //                log.debug("Show an explanation to the user");
            //                new Dialog().withPositiveButton().withMessage("Bla Bla Bla").show(getFragmentManager(),DIALOG_PERMISSION_WRITE);
            //
            //
            //                // Show an expanation to the user *asynchronously* -- don't block
            //                // this thread waiting for the user's response! After the user
            //                // sees the explanation, try again to request the permission.
            //
            //            } else {
            //
            //                log.debug("No explanation needed, we can request the permission.");
            //
            //
            //                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            //                // app-defined int constant. The callback method gets the
            //                // result of the request.
            //            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(ARG_IS_SHOWING_EXPLENATION_DIALOG, isShowingExplenationDialog);
        outState.putBoolean(ARG_IS_REQUESTING_PERMISSION, isRequestinPermission);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        log.debug("requestCode: {}, permissions: {}, grantResults: {}", requestCode, permissions, grantResults);
        isRequestinPermission = false;

        if (requestCode == PERMISSION_REQUEST_IMPORT_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dataFragment.restart();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    isShowingExplenationDialog = true;

                    new Dialog.Builder().setPositiveButton()
                                        .setNegativeButton()
                                        .setStyle(R.style.Dialog)
                                        .setMessage(R.string.dialog_import_permission_explanation)
                                        .build()
                                        .showAllowStateLoss(getFragmentManager(), DIALOG_PERMISSION_WRITE);
                }
            }
        }
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.start_import, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new ImportRecyclerAdapter();
        recyclerView.setAdapter(adapter);

        return view;
    }


    private void loadDirectory(File dir) {
        log.debug("dir: {}", dir);
        dataFragment.setCurrentDir(dir);
        dataFragment.restart();
    }

    public void onListClick(ImportDirectoryLoader.ImportFileWrapper ifw) {
        log.debug("ifw: {}", ifw);
        switch (ifw.getType()) {
            case FILE:
                if (!(ifw.getFile() == null || !ifw.getFile()
                                                   .exists())) {
                    Intent importIntent = new Intent(getActivity(), ImportActivity.class);
                    importIntent.setData(Uri.fromFile(ifw.getFile()));
                    startActivityForResult(importIntent, ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY);
                }
                break;
            case DIRECTORY:
                loadDirectory(ifw.getFile());
                break;
            case PARENTDIRECTORY:
                loadDirectory(dataFragment.getCurrentDir()
                                          .getParentFile());
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        log.debug("requestCode: {}, resultCode: {}, data: {}", requestCode, resultCode, data);
        if (requestCode == ImportActivity.REQUEST_CODE_IMPORT_ACTIVITY) {
            dataFragment.restart();
            if (resultCode == Activity.RESULT_OK) {
            }
        }
    }

    @Override
    public void dataChanged() {
        adapter.notifyDataSetChanged();
    }

    private class ImportRecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public int getItemCount() {
            if (dataFragment.getData() == null) return 0;
            return dataFragment.getData()
                               .size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (ImportDirectoryLoader.ImportFileWrapper.TYPE.values()[viewType]) {
                case PARENTDIRECTORY:
                    return new ParentDirViewHolder(LayoutInflater.from(parent.getContext())
                                                                 .inflate(R.layout.start_import_parentdirectory, parent, false));
                case DIRECTORY:
                    return new DirViewHolder(LayoutInflater.from(parent.getContext())
                                                           .inflate(R.layout.start_import_directory, parent, false));
                case FILE:
                    return new FileViewHolder(LayoutInflater.from(parent.getContext())
                                                            .inflate(R.layout.start_import_file, parent, false));
                case WAIT:
                    return new WaitViewHolder(LayoutInflater.from(parent.getContext())
                                                            .inflate(R.layout.start_import_wait, parent, false));

            }

            return null;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final ImportDirectoryLoader.ImportFileWrapper ifw = dataFragment.getData()
                                                                            .get(position);

            if (ifw.isSelectable()) {
                holder.setClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onListClick(ifw);
                    }
                });
            } else {
                holder.setNotClickable();
            }

            switch (ifw.getType()) {
                case FILE:
                    ((FileViewHolder) holder).textDetail.setText(dataFragment.getData()
                                                                             .get(position)
                                                                             .getDetail());


                    if (ifw.isOverride() || !ifw.isSelectable()) {
                        holder.image.clearColorFilter();
                    } else {
                        holder.image.setColorFilter(getResources().getColor(R.color.color_primary));
                    }
                case DIRECTORY:

                    holder.text.setText(dataFragment.getData()
                                                    .get(position)
                                                    .getName());
                    break;

            }
        }

        @Override
        public int getItemViewType(int position) {
            return dataFragment.getData()
                               .get(position)
                               .getType()
                               .ordinal();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        TextView text;
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setClickable(true);
            text = (TextView) itemView.findViewById(R.id.text);
            image = (ImageView) itemView.findViewById(R.id.image);
        }

        public void setClickListener(View.OnClickListener onClickListener) {
            itemView.setClickable(true);
            itemView.setOnClickListener(onClickListener);
            itemView.setEnabled(true);
        }

        public void setNotClickable() {
            itemView.setClickable(false);
            itemView.setOnClickListener(null);
            itemView.setEnabled(false);
        }

    }

    private class DirViewHolder extends ViewHolder {
        public DirViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class ParentDirViewHolder extends ViewHolder {
        public ParentDirViewHolder(View itemView) {
            super(itemView);
        }
    }

    private class FileViewHolder extends ViewHolder {
        TextView textDetail;

        public FileViewHolder(View itemView) {
            super(itemView);
            textDetail = (TextView) itemView.findViewById(R.id.textDetail);
        }
    }

    private class WaitViewHolder extends ViewHolder {
        private WaitViewHolder(View itemView) {
            super(itemView);
        }
    }

}
