package br.com.tchars.phold.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import br.com.tchars.phold.R;

public class MainActivity extends AppCompatActivity {

    ImageView imagemEscolhida;
    Button btnEscolherImagem;

    Bitmap bmp;
    Uri uriImagemSelecionada;
    InputStream imageStream;

    ColorMatrix matrix;

    SeekBar seekBarNivelDeSaturacao;

    private static final int CAMERA_CODE = 2504;
    private static final int GALERIA_CODE = 425;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        btnEscolherImagem = findViewById(R.id.btnAddFoto);
        imagemEscolhida = findViewById(R.id.imageView);

        btnEscolherImagem.setOnClickListener(v -> selecionarImagem());
        imagemEscolhida.setOnClickListener(v -> salvarImagem());

        seekBarNivelDeSaturacao = findViewById(R.id.seekBar);

        seekBarNivelDeSaturacao.setMin(0);
        seekBarNivelDeSaturacao.setMax(100);
        seekBarNivelDeSaturacao.setProgress(50);

        seekBarNivelDeSaturacao.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                matrix = new ColorMatrix();
                matrix.setSaturation(progress / 10);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                imagemEscolhida.setColorFilter(filter);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), "Clique na foto para salvar na galeria", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selecionarImagem() {

        final CharSequence[] opcoes = { "Câmera", "Galeria", "Cancelar" };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("Adicionar imagem");

        builder.setItems(opcoes, (dialog, which) -> {
            if (opcoes[which].equals("Câmera")) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(intent, CAMERA_CODE);

                    }
                    else {
                        /* Exibe a tela para o usuário dar a permissão. */
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[] {
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                }, CAMERA_CODE);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            else if (opcoes[which].equals("Galeria")) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent.createChooser(intent, "Selecione uma foto"), GALERIA_CODE);
            }
            else {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case CAMERA_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(getApplicationContext(), "Permissão aceita!", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_CODE) {
            Bundle bundle = data.getExtras();
            bmp = (Bitmap) bundle.get("data");

            imagemEscolhida.setImageBitmap(bmp);
        }
        else if (requestCode == GALERIA_CODE) {
            uriImagemSelecionada = data.getData();
            imagemEscolhida.setImageURI(uriImagemSelecionada);

            try {
                imageStream = getContentResolver().openInputStream(uriImagemSelecionada);
                bmp = BitmapFactory.decodeStream(imageStream);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap aplicarMatrix(Bitmap original) {

        Bitmap bitmap = Bitmap.createBitmap(original.getWidth(),
                original.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(original, 0, 0, paint);

        return bitmap;
    }

    private void salvarImagem() {
        if (bmp == null) {
            Toast.makeText(getApplicationContext(), "Imagem vazia", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap matrixAplicada = aplicarMatrix(bmp);

        FileOutputStream outputStream;
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        String nome = String.format("fotoEditada_%d.png", System.currentTimeMillis());
        File outFile = new File(path, nome);

        try {
            path.mkdirs();
            outputStream = new FileOutputStream(outFile);
            matrixAplicada.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(getApplicationContext(), "Foto salva!", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}