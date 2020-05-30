package com.example.billiorbillu;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {


    ImageView imageView;
    FloatingActionButton button;
    TextView textView;
    int action = 0;
    Interpreter tflite;
    Bitmap finalBitmap;
    ByteBuffer bytebuffer_float;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
            finalBitmap = Bitmap.createScaledBitmap(imageBitmap,200,200,false);
            bytebuffer_float = convertBitmapToByteBuffer_float(finalBitmap, 1,
                    200, 3);
            button.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_done_black));
            textView.setText("Submit the image to make prediction");
            action = 1;
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1);
        }
    }

    private void runModel(ByteBuffer img) {
        float[][] result = new float[1][1];
        tflite.run(img, result);
        action = 0;
        textView.setText("Take a picture to predict again");
        button.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_camera));
        imageView.setImageDrawable(null);
        boolean cat=true;
        if(result[0][0]>0.5){
            cat = false;
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        if(cat){
            dialog.setMessage("You look "+ (100-(int)(result[0][0]*100)) +"% like a cat!!!");
        }else{
            dialog.setMessage("You look "+(int)(result[0][0]*100) +"% like a dog!!!");
        }
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
        System.out.println(result[0][0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
        action = 0;
        textView = findViewById(R.id.textView);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (action == 0) {
                    dispatchTakePictureIntent();
                } else if (action == 1) {
                    runModel(bytebuffer_float);
                }
            }
        });
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("mobileModel.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private ByteBuffer convertBitmapToByteBuffer_float(Bitmap bitmap, int
            BATCH_SIZE, int inputSize, int PIXEL_SIZE) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * BATCH_SIZE *
                inputSize * inputSize * PIXEL_SIZE); //float_size = 4 bytes
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];


                byteBuffer.putFloat( ((val >> 16) & 0xFF)* (1.f/255.f));
                byteBuffer.putFloat( ((val >> 8) & 0xFF)* (1.f/255.f));
                byteBuffer.putFloat( (val & 0xFF)* (1.f/255.f));
            }
        }
        return byteBuffer;
    }
}
