package com.samedtemiz.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.samedtemiz.artbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;

    //Bir izin ve işlem yapıldığında ne olacağını yazmak istiyorsak bunu kullanıyoruz
    ActivityResultLauncher<Intent> activityResultLauncher;  //Galeriye gitmek için kullanıcaz
    ActivityResultLauncher<String> permissionLauncher;      //İzin istemek için kullanıcaz

    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equals("new")){
            //new art
            binding.txtArtName.setText("");
            binding.txtArtArtistName.setText("");
            binding.txtArtYear.setText("");
            binding.btnSave.setVisibility(View.VISIBLE);
            binding.detailButtons.setVisibility(View.INVISIBLE);
        }else{
            //existing art
            binding.btnSave.setVisibility(View.INVISIBLE);
            binding.detailButtons.setVisibility(View.VISIBLE);
            int artId = intent.getIntExtra("artId", 0);

            try{

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[]{String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("paintername");
                int artYearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while(cursor.moveToNext()){
                    binding.txtArtName.setText(cursor.getString(artNameIx));
                    binding.txtArtArtistName.setText(cursor.getString(artistNameIx));
                    binding.txtArtYear.setText(cursor.getString(artYearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    //Art Save
    public void save(View view) {
        String artName = binding.txtArtName.getText().toString();
        String artistName = binding.txtArtArtistName.getText().toString();
        String artYear = binding.txtArtYear.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1, artName);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3, artYear);
            sqLiteStatement.bindBlob(4, byteArray);

            sqLiteStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

        returnMain();
    }

    //Art details update
    public void update(View view){
        Intent intent = getIntent();
        int artId = intent.getIntExtra("artId", 0);

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        ContentValues cv = new ContentValues();
        cv.put("artname", binding.txtArtName.getText().toString());
        cv.put("paintername", binding.txtArtArtistName.getText().toString());
        cv.put("year", binding.txtArtYear.getText().toString());
        cv.put("image", byteArray);



        database.update("arts", cv, "id = ?", new String[]{String.valueOf(artId)});
        Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show();

        returnMain();
    }

    //Art delete
    public void delete(View view){
        Intent intent = getIntent();
        int artId = intent.getIntExtra("artId", 0);

        database.execSQL("DELETE FROM arts WHERE id = ?", new String[] {String.valueOf(artId)});

        Toast.makeText(this, "Deletion successful", Toast.LENGTH_SHORT).show();

        returnMain();
    }

    //Select art image
    public void selectImage(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android 33+ -> READ_MEDIA_IMAGES

            //READ_EXTERNAL_STORAGE izni verilmiş mi kontrol ediyoruz.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                //İzin istenirken neden istenildiğini gösteriyoruz
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {

                    //İzinin ismi ve tıklanması için buton ekliyoruz.
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Request permission
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();

                } else {
                    //Request permission -> Verilmemiş daha önce
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                //stuff.. -> İzin daha önce verilmiş

                //Intent.ACTION_PICK: Aksiyon olarak Pick işlemi yapılacağını söyledik
                //MediaStore.Images.Media.EXTERNAL_CONTENT_URI: Seçilen öğenin uri
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);

            }

        } else {
            //Android 32- -> READ_EXTERNAL_STORAGE

            //READ_EXTERNAL_STORAGE izni verilmiş mi kontrol ediyoruz.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //İzin istenirken neden istenildiğini gösteriyoruz
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    //İzinin ismi ve tıklanması için buton ekliyoruz.
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Request permission
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();

                } else {
                    //Request permission -> Verilmemiş daha önce
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            } else {
                //stuff.. -> İzin daha önce verilmiş

                //Intent.ACTION_PICK: Aksiyon olarak Pick işlemi yapılacağını söyledik
                //MediaStore.Images.Media.EXTERNAL_CONTENT_URI: Seçilen öğenin uri
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);

            }
        }

    }

    //ActivityResultLauncher nesnelerinin ne yapacağını burada tanımlıyoruz(register)
    private void registerLauncher() {

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    //Kullanıcı galeriden bir şey seçmiş
                    Intent intentFromResult = result.getData();

                    //Seçilen öğe null değilse
                    if (intentFromResult != null) {
                        Uri imageData = intentFromResult.getData();
//                        binding.imgSelect.setImageURI(imageData);

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);

                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                //result true ise izin verildi false ise verilmedi
                if (result) {
                    //permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                } else {
                    //permission denied
                    Toast.makeText(ArtActivity.this, "Permission needed!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public Bitmap makeSmallerImage(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            //Landscape image
            width = maxSize;
            height = (int) (width / bitmapRatio);

        } else {
            //Portrait image
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void returnMain(){
        //Ana sayfaya dönüyoruz
        Intent intent = new Intent(ArtActivity.this, MainActivity.class);

        //Bu dahil tüm activityleri kapat
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //Yeni activity açıyoruz
        startActivity(intent);
    }
}