package com.codewithash.redblogs;

import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DescriptionActivity extends AppCompatActivity {
    private Toolbar descriptionToolbar;
    private TextView descActivityTitle;
    private TextView descAvtivityDesc;
    private ImageView descActivityImage;
    private Button descActivityAudioBtn;
    private String blog_post_id;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private TextToSpeech textToSpeech;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        descriptionToolbar = findViewById(R.id.desc_toolbar);
        setSupportActionBar(descriptionToolbar);
        getSupportActionBar().setTitle("RedBlogs");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        blog_post_id = getIntent().getStringExtra("blog_post_id");
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int lang = textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }

        });
        descActivityAudioBtn = findViewById(R.id.desc_activity_audiobtn);
        descActivityAudioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = descAvtivityDesc.getText().toString();
                int speech = textToSpeech.speak(s,TextToSpeech.QUEUE_FLUSH,null);

            }
        });
        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser()!=null) {
            firebaseFirestore = FirebaseFirestore.getInstance();
            firebaseFirestore.collection("Posts").document(blog_post_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        String title = task.getResult().getString("title");
                        String desc = task.getResult().getString("desc");
                        String imageurl = task.getResult().getString("image_url");
                        setBlogPost(title,desc,imageurl);
                    } else {
                        String error = task.getException().getMessage();
                        Toast.makeText(DescriptionActivity.this,error,Toast.LENGTH_LONG).show();
                    }
                }
            });

        }


    }
    @Override
    protected void onStop()
    {
        super.onStop();

        if(textToSpeech != null){
            textToSpeech.shutdown();
        }
    }

    private void setBlogPost(String title, String desc, String imageurl) {
        descActivityTitle = findViewById(R.id.desc_activity_title);
        descActivityImage = findViewById(R.id.desc_activity_image);
        descAvtivityDesc = findViewById(R.id.desc_activity_desc);
        descActivityTitle.setText(title);
        descAvtivityDesc.setText(desc);
        RequestOptions placeholderOption = new RequestOptions();
        placeholderOption.placeholder(R.drawable.image_placeholder);
        Glide.with(DescriptionActivity.this).applyDefaultRequestOptions(placeholderOption).load(imageurl).into(descActivityImage);
    }
}
