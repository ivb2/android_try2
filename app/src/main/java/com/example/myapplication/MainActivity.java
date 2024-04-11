package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.media.MediaScannerConnection;
import android.net.Uri;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "MainActivity";

    private Button btnChooseImage, btnCompress;
    private Spinner spinnerCompressionMethod;
    private SeekBar seekBarCompressionLevel;
    private Uri imageUri;

    private EditText editTextCompressionLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnCompress = findViewById(R.id.btnCompress);
        spinnerCompressionMethod = findViewById(R.id.spinnerCompressionMethod);
        seekBarCompressionLevel = findViewById(R.id.seekBarCompressionLevel);

        // Nastavení adapteru pro spinner s komprimačními metodami
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.compression_methods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCompressionMethod.setAdapter(adapter);

        // Nastavení listeneru pro výběr obrázku
        btnChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        // Nastavení listeneru pro tlačítko komprimovat
        btnCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compressImage();
            }
        });


        editTextCompressionLevel = findViewById(R.id.editTextCompressionLevel);

        // Nastavení posluchače pro změnu hodnoty posuvníku
        seekBarCompressionLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Aktualizace textového pole s procentuální hodnotou
                editTextCompressionLevel.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Nastavení posluchače pro změnu hodnoty textového pole
        editTextCompressionLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Získání hodnoty z textového pole a nastavení hodnoty posuvníku
                String percentageStr = s.toString().replace("%", "");
                try {
                    int percentage = Integer.parseInt(percentageStr);
                    if (percentage >= 0 && percentage <= 100) {
                        seekBarCompressionLevel.setProgress(percentage);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });



    }

    // Metoda pro výběr obrázku z galerie
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Vyberte obrázek"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Log.d(TAG, "Selected Image URI: " + imageUri.toString());
        }
    }



    // Metoda pro komprimaci a uložení obrázku
    private void compressImage() {
        if (imageUri == null) {
            Toast.makeText(this, "Vyberte prosím obrázek nejprve.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Získání bitmapy z URI obrázku
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Získání komprimační metody
        String compressionMethod = spinnerCompressionMethod.getSelectedItem().toString();

        // Získání úrovně komprese
        int compressionLevel = seekBarCompressionLevel.getProgress();

        // Komprese obrázku podle zvolené metody a úrovně komprese
        Bitmap compressedBitmap = null;
        if (compressionMethod.equals("Metoda 1")) {
            compressedBitmap = compressBitmapMethod1(bitmap, compressionLevel);
        } else if (compressionMethod.equals("Metoda 2")) {
            compressedBitmap = compressBitmapMethod2(bitmap, compressionLevel);
        } else if (compressionMethod.equals("Metoda 3")) {
            compressedBitmap = compressBitmapMethod3(bitmap, compressionLevel);
        }

        // Uložení komprimované bitmapy do složky pictures/compressed v interním úložišti zařízení
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File compressedDir = new File(picturesDir, "compressed");
        if (!compressedDir.exists()) {
            compressedDir.mkdirs(); // Vytvoření složky, pokud ještě neexistuje
        }
        File file = new File(compressedDir, "compressed_image.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            // Aktualizace mediálního úložiště
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

            Toast.makeText(this, "Obrázek byl úspěšně komprimován a uložen.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Chyba při ukládání obrázku.", Toast.LENGTH_SHORT).show();
        }
    }






    // Metoda pro komprimaci obrázku pomocí metody 1 (změna kvality)
    private Bitmap compressBitmapMethod1(Bitmap bitmap, int compressionLevel) {
        int quality = 100 - compressionLevel; // Získání kvality komprese (0 - 100)
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream); // Komprese s určitou kvalitou
        byte[] byteArray = stream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    // Metoda pro komprimaci obrázku pomocí metody 2 (změna rozlišení)
    private Bitmap compressBitmapMethod2(Bitmap bitmap, int compressionLevel) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (int) (width * (1 - compressionLevel / 100f)); // Změna šířky podle úrovně komprese
        int newHeight = (int) (height * (1 - compressionLevel / 100f)); // Změna výšky podle úrovně komprese
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true); // Vytvoření nové bitmapy se změněným rozlišením
    }

    // Metoda pro komprimaci obrázku pomocí metody 3 (algoritmus pro snížení velikosti)
    private Bitmap compressBitmapMethod3(Bitmap bitmap, int compressionLevel) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        float sizeFactor = 1 - (compressionLevel / 100f); // Faktor změny velikosti obrázku
        int targetSize = (int) (out.toByteArray().length * sizeFactor); // Nová cílová velikost
        int quality = 100; // Počáteční kvalita
        out.reset(); // Resetování streamu pro novou kompresi
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out); // Počáteční komprese s plnou kvalitou
        while (out.size() > targetSize && quality > 0) {
            out.reset(); // Resetování streamu
            quality -= 5; // Snížení kvality o 5 jednotek
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out); // Nová komprese s nižší kvalitou
        }
        byte[] byteArray = out.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

}
