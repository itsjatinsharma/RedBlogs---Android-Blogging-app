package com.codewithash.redblogs;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import id.zelory.compressor.Compressor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class NewPostActivity extends AppCompatActivity {
    private static final int MAX_LENGTH = 100;
    private Toolbar newPostToolbar;

    private ImageView newPostImage;
    private EditText newPostDesc;
    private EditText newPostTitle;
    private Button newPostBtn;

    private Uri postImageUri = null;

    private ProgressBar newPostProgress;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private Button speechToTextBtn;
    private String current_user_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        current_user_id = firebaseAuth.getCurrentUser().getUid();

        newPostToolbar = findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        newPostImage = findViewById(R.id.new_post_image);
        newPostDesc = findViewById(R.id.new_post_desc);
        newPostTitle = findViewById(R.id.new_post_title);
        newPostBtn = findViewById(R.id.post_btn);
        newPostProgress = findViewById(R.id.new_post_progress);
        speechToTextBtn = findViewById(R.id.speechToText_btn);
        speechToTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Listening...");
                try {
                    startActivityForResult(intent,1);
                } catch (ActivityNotFoundException e){
                    Toast.makeText(NewPostActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512)
                        .setAspectRatio(3, 2)
                        .start(NewPostActivity.this);

            }
        });
        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String desc = newPostDesc.getText().toString();
                final String title = newPostTitle.getText().toString();

                if(!TextUtils.isEmpty(desc) && !TextUtils.isEmpty(title) && postImageUri != null){
                    newPostProgress.setVisibility(View.VISIBLE);
                    String randomName = random();
                    final StorageReference filepath = storageReference.child("post_images").child(randomName+".jpg");
                    filepath.putFile(postImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if(!task.isSuccessful()){
                                throw task.getException();
                            }
                            return filepath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()){
                                String downloadUri = task.getResult().toString();
                                Map<String , Object> postMap = new HashMap<>();
                                postMap.put("image_url",downloadUri);
                                postMap.put("title",title);
                                postMap.put("desc",desc);
                                postMap.put("user_id",current_user_id);
                                postMap.put("timestamp",FieldValue.serverTimestamp());
                                firebaseFirestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(NewPostActivity.this, "Post was added", Toast.LENGTH_LONG).show();
                                            Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
                                            startActivity(mainIntent);
                                            finish();
                                        } else {
                                            String error = task.getException().getMessage();
                                            Toast.makeText(NewPostActivity.this, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();
                                            newPostProgress.setVisibility(View.INVISIBLE);
                                        }
                                        newPostProgress.setVisibility(View.INVISIBLE);
                                    }
                                });
                            } else {
                                String error = task.getException().getMessage();
                                Toast.makeText(NewPostActivity.this, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();
                                newPostProgress.setVisibility(View.INVISIBLE);
                            }

                        }

                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1 : {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    newPostDesc.setText(result.get(0));
                }
                break;
            }
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {

                    postImageUri = result.getUri();
                    newPostImage.setImageURI(postImageUri);

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                    Exception error = result.getError();

                }
                break;
            }

        }
    }
    public  static String random(){
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for(int i=0;i<randomLength;i++){
            tempChar = (char)(generator.nextInt(96)+32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
