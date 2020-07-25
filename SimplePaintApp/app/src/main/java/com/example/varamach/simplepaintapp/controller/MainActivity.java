package com.example.varamach.simplepaintapp.controller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.MenuInflater;
import android.view.View;

import com.example.varamach.simplepaintapp.gridview;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.varamach.simplepaintapp.R;
import com.example.varamach.simplepaintapp.model.SavedArt;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * Main Activity handles a view which allows to create an artwork, show saved artworks and option to
 * open or delete saved items. It also handles a model which stores the list of saved artworks.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AbsListView.MultiChoiceModeListener {


    private final static int ACTIVITY_ID = 100;
    public static final int CAMERA_REQUEST_CODE = 2;
    public static final int STORAGE_REQUEST_CODE = 4;
    public static final int IMAGE_PICK_GALLERY_CODE = 6;
    public static final int IMAGE_PICK_CAMERA_CODE = 8;
    ImageView imageView;

    String[] cameraPermission, storagePermission;
    Uri imageUri;

    private ListView mMainListView ;
    private ArrayAdapter<String> mListAdapter ;
    private ArrayList<String> mSelectedForDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Setup main layout
        setContentView(R.layout.activity_main);


        //Setup drawer layout
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        //Setup the navigation and the layout when drawer is opened
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //CAMERA PERMISSION
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //STORAGE PERMISSION
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //Setup 'Add Art' SpeedDialView Fab button
        final SpeedDialView addArtButton = findViewById(R.id.id_Fab);
        addArtButton.inflate(R.menu.fab_menu);

        addArtButton.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem actionItem) {
                switch (actionItem.getId()) {
                    case R.id.id_camera:
                        //CHECKING PERMISSION FOR CAMERA
                        if (!checkCameraPermission()){
                            //IF NOT GRANTED THEN REQUEST
                            requestCameraPermission();
                        }else{
                            //IF GRANDED THEN CAPTURE
                            pickCamera();
                        }
                        return true; // true to keep the Speed Dial open
                    case R.id.id_gallery:
                        //CHECKING PERMISSION FOR GALLERY
                        if(!checkStoragePermission())
                        {
                            //IF NOT GRANTED THEN REQUEST
                            requestStoragePermission();
                        }
                        else
                        {
                            //IF GRANDED THEN PICK IMAGE FROM GALLEY
                            pickGallery();
                        }
                        return true; // true to keep the Speed Dial open
                    case R.id.id_draw:
                        Intent intent = new Intent(MainActivity.this, PaintActivity.class);
                        startActivityForResult(intent, ACTIVITY_ID);
                        addArtButton.close();
                        return true; // true to keep the Speed Dial open
                    default:
                        return false;
                }
            }
        });


        //Restore item selected for delete.
        if (savedInstanceState != null) {
            mSelectedForDelete = savedInstanceState.getStringArrayList("selectedForDelete");
        } else {
            mSelectedForDelete = new ArrayList<>();
        }

        //Retrieve/Restore saved art names
        LinkedList<String> artNames = SavedArt.getInstance(this).getArtNames();
        refreshHelperText(artNames);

        //Main list view
        mMainListView = findViewById(R.id.main_list_view);
        mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_activated_1, artNames);
        mMainListView.setAdapter( mListAdapter );
        mMainListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mMainListView.setMultiChoiceModeListener(this);
        mMainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // ListView Clicked item value
                String  paintName    = (String) mMainListView.getItemAtPosition(position);

                // Show Activity
                Intent intent = new Intent(MainActivity.this, PaintActivity.class);
                intent.putExtra("paintName", paintName);
                startActivity(intent);
            }
        });
    }

    private void pickGallery() {
        //INTENT TO PICK IMAGES FROM GALLERY
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        //SET INTENT TYPE TO IMAGES
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickCamera() {
        //INTENT TO TAKE IMAGES FROM CAMERA ALSO WE WILL FIRST SAVE IMAGES INTO GALLERY TO GET HIGH RESOLUTION IMAGES
        ContentValues contentValues = new ContentValues();
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(cameraAccepted && writeStorageAccepted){
                        pickCamera();
                    }else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length>0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if(writeStorageAccepted){
                        pickGallery();
                    }else{
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save item selected for delete
        outState.putStringArrayList("selectedForDelete", mSelectedForDelete);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Added to handle new art creation.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_ID: {
                if (resultCode == Activity.RESULT_OK) {
                    boolean added = data.getBooleanExtra(getString(R.string.saved), false);
                    if (added) {
                        //When new one added, see if we need to hide the information textview.
                        LinkedList<String> artNames = SavedArt.getInstance(this).getArtNames();
                        refreshHelperText(artNames);
                    }
                }
                break;
            }
            case IMAGE_PICK_GALLERY_CODE: {
                if (resultCode == RESULT_OK) {
                    if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                        // Image selected from Gallery and now crop!

                        CropImage.activity(data.getData())
                                //.setAspectRatio(48,48)
                                .setFixAspectRatio(true)
                                //.setRequestedSize(48,48)
                                .setAllowCounterRotation(true)
                               // .setMaxCropResultSize(48,48)
                                .setGuidelines(CropImageView.Guidelines.ON) // guideline enable
                                .start(this);


                    }
                }
                break;
            }
            case IMAGE_PICK_CAMERA_CODE: {
                if (resultCode == RESULT_OK) {
                    if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                        // Image clicked from Camera

                        CropImage.activity(imageUri)
                                .setGuidelines(CropImageView.Guidelines.ON) // guideline enable
                                .start(this);
                    }
                }
                break;
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            //CropImage.ActivityResult result = CropImage.getActivityResult(data);
                CropImage.ActivityResult pic = CropImage.getActivityResult(data);
                Bitmap bitmap = pic.getBitmap();
                //Uri resultUri = pic.getUri();
                Intent intent = new Intent(this, gridview.class);
                startActivity(intent);
                //CropImageView cropImageView = findViewById(R.id.id_cropImage);
                ImageView imageView2 = findViewById(R.id.imageView2);
                imageView2.setImageBitmap(bitmap);
                //cropImageView.setImageBitmap(bitmap);
                //cropImageView.setImageUriAsync(resultUri);
                //cropImageView.setImageUriAsync(resultUri);
                //cropImageView.getCroppedImageAsync();

            }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    protected void onStop() {
        super.onStop();
        //Save the art names.
        SavedArt.getInstance(this).persistArtNames(this);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            //Show About dialog
            new AlertDialog.Builder(this)
                    .setMessage(R.string.about_message)
                    .setTitle(R.string.about_title)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //CAB Handlers
    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedForDelete.add(mListAdapter.getItem(position));
        } else {
            mSelectedForDelete.remove(mListAdapter.getItem(position));
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.listitem_cab, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_delete:
                //Remove selected list items
                for (String item : mSelectedForDelete) {
                    SavedArt.getInstance(this).removeArt(item);
                }

                //See if helper text needs to be shown
                LinkedList<String> artNames = SavedArt.getInstance(this).getArtNames();
                refreshHelperText(artNames);

                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    private void refreshHelperText( LinkedList<String> artNames) {
        TextView textView = findViewById(R.id.main_text_view);
        textView.setVisibility(artNames == null || artNames.size() == 0 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        mSelectedForDelete.clear();
    }

}
