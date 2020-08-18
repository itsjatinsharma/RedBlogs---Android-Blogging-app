package com.codewithash.redblogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.hdodenhof.circleimageview.CircleImageView;

public class BlogRecyclerAdapter extends RecyclerView.Adapter<BlogRecyclerAdapter.ViewHolder> {

    public List<BlogPost> blog_list;
    public Context context;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    public BlogRecyclerAdapter(List<BlogPost> blog_list){
        this.blog_list=blog_list;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blog_list_item,parent,false);
        context = parent.getContext();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        final String blogPostId = blog_list.get(position).BlogPostId;
        final String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String title_data = blog_list.get(position).getTitle();
        holder.setTitleText(title_data);
        String image_url = blog_list.get(position).getImage_url();
        holder.setBlogImage(image_url);
        final String user_id = blog_list.get(position).getUser_id();
        if(user_id.equals(currentUserId)){
            holder.blogDeleteBtn.setEnabled(true);
            holder.blogDeleteBtn.setVisibility(View.VISIBLE);
        }
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful()){
                    String userName = task.getResult().getString("name");
                    String userImage = task.getResult().getString("image");

                    holder.setUserData(userName, userImage);
                } else {
                    String error = task.getException().getMessage();
                   // Toast.makeText(, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();
                    Log.i("Error",error);
                }
            }
        });
        long millisecond = blog_list.get(position).getTimestamp().getTime();
        String dateString = new SimpleDateFormat("MM/dd/yyyy").format(new Date(millisecond));
        holder.setTime(dateString);
        //Likes Count
        firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException e) {
                if(e==null) {
                    if (!documentSnapshots.isEmpty()) {
                        int count = documentSnapshots.size();
                        holder.updateLikesCount(count);
                    } else {
                        holder.updateLikesCount(0);
                    }
                }
            }
        });
        firebaseFirestore.collection("Posts/"+blogPostId+"/Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot documentSnapshots, @Nullable FirebaseFirestoreException e) {
                if(e==null) {
                    if (!documentSnapshots.isEmpty()) {
                        int count = documentSnapshots.size();
                        holder.updateCommentCount(count);
                    } else {
                        holder.updateCommentCount(0);
                    }
                }
            }
        });
        // Get Likes
        firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(e==null) {
                    if (documentSnapshot.exists()) {
                        holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_accent));
                    } else {
                        holder.blogLikeBtn.setImageDrawable(context.getDrawable(R.mipmap.action_like_gray));
                    }
                }
            }
        });
        // Likes Feature
        holder.blogLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(!task.getResult().exists()){
                            Map<String, Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp", FieldValue.serverTimestamp());
                            firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).set(likesMap);
                        } else {
                            firebaseFirestore.collection("Posts/"+ blogPostId + "/Likes").document(currentUserId).delete();
                        }
                    }
                });

            }
        });
        holder.titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent descriptionIntent = new Intent(context,DescriptionActivity.class);
                descriptionIntent.putExtra("blog_post_id",blogPostId);
                context.startActivity(descriptionIntent);
            }
        });
        holder.blogCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent commentIntent = new Intent(context,CommentsActivity.class);
                commentIntent.putExtra("blog_post_id",blogPostId);
                context.startActivity(commentIntent);
            }
        });
        holder.blogDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseFirestore.collection("Posts").document(blogPostId).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        blog_list.remove(position);
                        Toast.makeText(context,"Post Deleted !!!",Toast.LENGTH_SHORT).show();
                        //

                        ((Activity)context).finish();
                        Intent intent = new Intent(context,MainActivity.class);
                        context.startActivity(intent);

                        //
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return blog_list.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder {
        private View mView;
        private TextView titleView;
        private ImageView blogImageView;
        private TextView blogDate;
        private TextView blogUserName;
        private CircleImageView blogUserImage;
        private ImageView blogLikeBtn;
        private TextView  blogLikeCount;
        private ImageView blogCommentBtn;
        private TextView blogCommentCount;
        private ImageView blogDeleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            blogLikeBtn = mView.findViewById(R.id.blog_like_btn);
            blogCommentBtn = mView.findViewById(R.id.blog_comment_icon);
            blogDeleteBtn = mView.findViewById(R.id.blog_delete_btn);
        }

        public void setTitleText(String titleText){
            titleView = mView.findViewById(R.id.blog_title);
            titleView.setText(titleText);
        }

        public void setBlogImage(String downloadUri){
            blogImageView = mView.findViewById(R.id.blog_image);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.image_placeholder);
            Glide.with(context).applyDefaultRequestOptions(requestOptions).load(downloadUri).into(blogImageView);
        }

        public void setTime(String date) {

            blogDate = mView.findViewById(R.id.blog_date);
            blogDate.setText(date);

        }
        public void setUserData(String name, String image){

            blogUserImage = mView.findViewById(R.id.blog_user_image);
            blogUserName = mView.findViewById(R.id.blog_user_name);

            blogUserName.setText(name);

            RequestOptions placeholderOption = new RequestOptions();
            placeholderOption.placeholder(R.drawable.profile_placeholder);

            Glide.with(context).applyDefaultRequestOptions(placeholderOption).load(image).into(blogUserImage);

        }
        public void updateLikesCount(int count){
            blogLikeCount = mView.findViewById(R.id.blog_like_count);
            blogLikeCount.setText(count+" Likes");
        }
        public void updateCommentCount(int count){
            blogCommentCount = mView.findViewById(R.id.blog_comment_count);
            blogCommentCount.setText(count+" Comments");
        }
    }
}
