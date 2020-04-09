package com.example.filemanager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout1);
    }

    private boolean isFileManagerInitialized;
    private boolean[] filesSelection;
    private File[] filesArray;
    private List<String> filesList;
    private int filesFoundCount;
    private Button buttonRefresh;
    private File dir;
    private String currentPath;
    private String currentFolder;
    private boolean isLongClick;
    private int selectedItemIndex;
    private String copyPath;

    private Button buttonTakePicture;
    private String pathToFile;

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arePermissionsDenied() ) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        if (!isFileManagerInitialized){
            //Set default directory
            currentPath = String.valueOf(Environment.
                    getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            final String rootPath = currentPath.substring(0, currentPath.lastIndexOf('/'));

            final TextView pathOutput = findViewById(R.id.pathOutput);
            final ListView listView = findViewById(R.id.listView);
            final TextAdapter textAdapter1 = new TextAdapter();
            listView.setAdapter(textAdapter1);
            filesList = new ArrayList<>();

            buttonRefresh = findViewById(R.id.buttonRefresh);
            buttonRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //For just the folder we're currently in (in heading)
                    currentFolder = currentPath.substring(currentPath.lastIndexOf('/') + 1);
                    pathOutput.setText(currentFolder);
                    dir = new File(currentPath);
                    filesArray = dir.listFiles();
                    filesFoundCount = filesArray.length;
                    filesSelection = new boolean[filesFoundCount];
                    textAdapter1.setSelection(filesSelection);
                    filesList.clear();
                    for (int i = 0; i < filesFoundCount; i++) {
                        filesList.add(String.valueOf(filesArray[i].getAbsolutePath()));
                    }
                    textAdapter1.setData(filesList);
                }
            });

            buttonRefresh.callOnClick();

            final Button buttonGoBack = findViewById(R.id.buttonGoBack);
            buttonGoBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPath.equals(rootPath)){
                        return;
                    }
                    currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
                    buttonRefresh.callOnClick();
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //check if flag is set
                            //Wait 200 millisec to see if longclick isn't called
                            if(!isLongClick){
                                if(filesArray[position].isDirectory()) {
                                    currentPath = filesArray[position].getAbsolutePath();
                                    buttonRefresh.callOnClick();
                                }
                            }
                        }
                    },50);
                }
            });
            //handle selection/deselection
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                //For non-long click,
                //replace the listener with setOnItemClickListener
                //change below method to void onItemClick
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    isLongClick = true;
                    //if it's selected, then unselect it, and vice versa
                    filesSelection[position] = !filesSelection[position];
                    textAdapter1.setSelection(filesSelection);
                    int selectionCount = 0;

                    for (boolean aSelection : filesSelection){
                        if (aSelection) {
                            selectionCount++;
                        }
                    }

                    /*
                    for (int i = 0; i < filesSelection.length; i++){
                        if(filesSelection[i]){
                            selectionCount++;
                        }
                    }
                    */

                    if(selectionCount > 0){
                        if (selectionCount == 1){
                            selectedItemIndex = position;
                            findViewById(R.id.buttonRename).setVisibility(View.VISIBLE);
                            //To fix by myself -
                            //The following code disables the copy button displaying for folders
                            //(since folder copying doesn't work)
                            if(!filesArray[selectedItemIndex].isDirectory()){
                                findViewById(R.id.buttonCopy).setVisibility(View.VISIBLE);
                            }
                        }
                        else{
                            findViewById(R.id.buttonRename).setVisibility(View.GONE);
                        }
                        findViewById(R.id.bottomBar).setVisibility(View.VISIBLE);
                    }
                    else{
                        findViewById(R.id.bottomBar).setVisibility(View.GONE);
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            isLongClick = false;
                        }
                    },1000);
                    return false;
                }
            });
            final Button buttonDelete = findViewById(R.id.buttonDelete);
            final Button buttonRename = findViewById(R.id.buttonRename);
            final Button buttonCopy = findViewById(R.id.buttonCopy);
            final Button buttonPaste = findViewById(R.id.buttonPaste);

            //final Button button5 = findViewById(R.id.button5);
            final Button buttonNewFolder = findViewById(R.id.buttonNewFolder);
            buttonDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                    deleteDialog.setTitle("Delete");
                    deleteDialog.setMessage("Do you really want to delete this?");
                    deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for(int i = 0; i < filesArray.length; i++){
                                if(filesSelection[i]){
                                    deleteFileOrFolder(filesArray[i]);
                                    filesSelection[i] = false;
                                }
                            }
                            buttonRefresh.callOnClick();
                        }
                    });
                    deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            buttonRefresh.callOnClick();
                        }
                    });
                    deleteDialog.show();
                }
            });


            buttonNewFolder.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final AlertDialog.Builder newFolderDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    newFolderDialog.setTitle("New Folder");
                    final EditText input =  new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    newFolderDialog.setView(input);
                    newFolderDialog.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final File newFolder = new File(currentPath + "/" + input.getText());
                                    if(!newFolder.exists()){
                                        newFolder.mkdir();
                                        Log.d("TA{ new folder = ", newFolder + "");
                                        buttonRefresh.callOnClick();
                                    }
                                }
                            }
                    );
                    newFolderDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }
                    );
                    newFolderDialog.show();
                }
            });

            buttonRename.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    final AlertDialog.Builder renameDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    renameDialog.setTitle("Rename to:");
                    final EditText input = new EditText(MainActivity.this);
                    final String renamePath = filesArray[selectedItemIndex].getAbsolutePath();
                    input.setText(renamePath.substring(renamePath.lastIndexOf("/")));
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    renameDialog.setView(input);
                    renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String s = new File(renamePath).getParent() + "/" + input.getText();
                            File newFile = new File(s);
                            new File(renamePath).renameTo(newFile);
                            buttonRefresh.callOnClick();
                            filesSelection = new boolean[filesArray.length];
                            textAdapter1.setSelection(filesSelection);
                        }
                    });
                    renameDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which){
                                dialog.cancel();
                                buttonRefresh.callOnClick();
                            }
                        }
                    );

                    renameDialog.show();
                }
            });

            buttonCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyPath = filesArray[selectedItemIndex].getAbsolutePath();
                        Log.d("Copy testing", copyPath);
                    filesSelection = new boolean[filesArray.length];
                    textAdapter1.setSelection(filesSelection);
                    findViewById(R.id.buttonPaste).setVisibility(View.VISIBLE);
                }
            });

            buttonPaste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonPaste.setVisibility(View.GONE);
                    String destPath = currentPath + copyPath.substring(copyPath.lastIndexOf("/"));
                        Log.d("Paste testing", destPath);
                    copy(new File(copyPath), new File(destPath));
                    buttonRefresh.callOnClick();
                }
            });

            buttonTakePicture = findViewById(R.id.buttonPhoto);
            buttonTakePicture.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    dispatchPictureTakeAction();
                }
            });



            isFileManagerInitialized = true;
        }
        else{
            buttonRefresh.callOnClick();
        }
    }

    private void dispatchPictureTakeAction(){
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(pictureIntent.resolveActivity(getPackageManager()) != null){
            File photoFile = null;
            photoFile = createPhotoFile();

            if(photoFile != null){
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.example.filemanager.android.fileprovider", photoFile);
                pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(pictureIntent, 1);

            }

        }
    }

    private File createPhotoFile(){
        String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        //File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String currentFileLocation = currentPath.substring(currentPath.indexOf('/', 19) + 1);
        Log.d("TA{ currFileLocation = ", currentFileLocation);
            //currentPath.substring(currentPath.lastIndexOf('/') + 1)
        //Note, cannot store images in root folder
            //Warn user of this!

        //File storageDir = getExternalStoragePublicDirectory(currentFolder);
        File storageDir = getExternalStoragePublicDirectory(currentFileLocation);

        Log.d("TA{ storageDir = ", storageDir + "");

        File image = null;
        try{
            image = new File(storageDir, name + ".jpg");
        } catch (Exception e){

        }
        Log.d("TA{ image = ", image + "");
        return image;
    }

    private void copy(File src, File dest){
        try {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;

            while((length = in.read(buffer)) > 0){
                out.write(buffer, 0, length);
            }

            out.close();
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    class TextAdapter extends BaseAdapter{
        private List<String> data = new ArrayList<>();
        private boolean[] filesSelection;
        public void setData(List<String> data){
            if(data != null){
                this.data.clear();
                if(data.size() > 0) {
                    this.data.addAll(data);
                }
                notifyDataSetChanged();
            }
        }
        void setSelection(boolean[] filesSelection){
            if(filesSelection != null){
                this.filesSelection = new boolean[filesSelection.length];
                for(int i = 0; i < filesSelection.length; i++){
                    this.filesSelection[i] = filesSelection[i];
                }
                notifyDataSetChanged();
            }
        }
        @Override
        public int getCount() {
            return data.size();
        }
        @Override
        public String getItem(int position) {
            return data.get(position);
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.textItem)));
            }
            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = getItem(position);
            //Displays the file address in full
               //holder.info.setText(item);
            //Displays just the file name itself
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            //handles background colouring for file selection/deselection
            if(filesSelection != null){
                if(filesSelection[position]) {
                    holder.info.setBackgroundColor(Color.argb(100, 9, 9, 9));
                }
                else{
                    holder.info.setBackgroundColor(Color.WHITE);
                }
            }
            return convertView;
        }
        class ViewHolder{
            TextView info;
            ViewHolder(TextView info){
                this.info = info;
            }
        }
    }
    private static final int REQUEST_PERMISSIONS = 1;
    private static final String[] PERMISSIONS = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_COUNT = 2;
    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){
        int p = 0;
        while (p < PERMISSIONS_COUNT){
            if (checkSelfPermission(PERMISSIONS[p]) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
            p++;
        }
        return false;
    }

    private void deleteFileOrFolder(File fileOrFolder){
        if(fileOrFolder.isDirectory()){
            if(fileOrFolder.list().length == 0){
                fileOrFolder.delete();
            }
            else{
                String filesArray[] = fileOrFolder.list();
                for(String temp : filesArray){
                    File fileToDelete = new File(fileOrFolder, temp);
                    deleteFileOrFolder(fileToDelete);
                }
                if (fileOrFolder.list().length == 0){
                    fileOrFolder.delete();
                }
            }
        }
        else{
            fileOrFolder.delete();
        }
    }
    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            //Keep asking for permissions without being locked by the Android system (prompt)
            if(arePermissionsDenied()){
                ((ActivityManager) Objects.requireNonNull(this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }
            else {
                onResume();
            }
        }
    }
}