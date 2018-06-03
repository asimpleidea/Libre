package mad24.polito.it.fragments.viewbook;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.service.notification.ConditionProviderService;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import mad24.polito.it.BooksActivity;
import mad24.polito.it.R;
import mad24.polito.it.fragments.SearchFragment;
import mad24.polito.it.models.Comment;

public class RecyclerViewAdapterComment extends RecyclerView.Adapter<RecyclerViewAdapterComment.MyViewHolder> {
    private static final String FIREBASE_DATABASE_LOCATION_USERS_PICTURES = "profile_pictures";
    private static final String FIREBASE_DATABASE_LOCATION_USERS = BooksActivity.FIREBASE_DATABASE_LOCATION_USERS;

    private Context mContext ;
    private List<Comment> mDataComments;
    private CommentDialog commentDialog;

    private View view;
    private StorageReference mStorageRef;


    public RecyclerViewAdapterComment(Context mContext, List<Comment> mDataComments, CommentDialog commentDialog) {
        this.mContext = mContext;
        this.mDataComments = mDataComments;
        this.commentDialog = commentDialog;

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.adapter_comment_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        /*StorageReference storageReference = mStorageRef.child(FIREBASE_DATABASE_LOCATION_USERS_PICTURES).child(mDataComments.get(position).getUser()+".jpg");

        storageReference.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String imageURL = uri.toString();

                        //  Set the url of cover *INSIDE* the object (so we won't have to query it again later).
                        //mData.get(holder.getAdapterPosition()).setBookImageLink(uri.toString());
                        //  IMPORTANT UPDATE: without this condition, an error is raised when clicking a notification.
                        if(mContext != null)
                            Glide.with(mContext).load(imageURL).into(holder.thumbnail);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });*/

        //using the method above, the images are not uploaded rotating the display
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(FIREBASE_DATABASE_LOCATION_USERS_PICTURES).child(mDataComments.get(position).getUser()+".jpg");
        try {
            final File localFile = File.createTempFile(mDataComments.get(position).getUser(), ".bmp");
            ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap profileImageBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    holder.thumbnail.setImageBitmap(profileImageBitmap);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child(FIREBASE_DATABASE_LOCATION_USERS)
                .child(mDataComments.get(position).getUser())
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String name = dataSnapshot.getValue(String.class);
                        holder.username.setText(name);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });


        holder.ratingBar.setRating(mDataComments.get(position).getStars());
        holder.comment.setText(mDataComments.get(position).getComment());
    }

    public void addAll(List<Comment> newComments) {
        int initialSize = mDataComments.size();
        mDataComments.addAll(newComments);
        notifyItemRangeInserted(initialSize, newComments.size());
    }

    @Override
    public int getItemCount() {
        return mDataComments.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        de.hdodenhof.circleimageview.CircleImageView thumbnail;
        TextView username;
        TextView comment;
        me.zhanghai.android.materialratingbar.MaterialRatingBar ratingBar;

        public MyViewHolder(View itemView) {
            super(itemView);

            thumbnail = (de.hdodenhof.circleimageview.CircleImageView) itemView.findViewById(R.id.comment_userPic) ;
            username = (TextView) itemView.findViewById(R.id.comment_userName);
            comment = (TextView) itemView.findViewById(R.id.comment_comment);
            ratingBar = (me.zhanghai.android.materialratingbar.MaterialRatingBar) itemView.findViewById(R.id.comment_ratingBar);
        }
    }

}
