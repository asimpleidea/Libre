package mad24.polito.it;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.List;

import mad24.polito.it.models.Book;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private StorageReference mStorageRef;
    private StorageReference coverRef;

    Context mContext;
    List<Book> mData;

    public RecyclerViewAdapter(Context mContext, List<Book> mData) {
        this.mContext = mContext;
        this.mData = mData;

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;
        v = LayoutInflater.from(mContext).inflate(R.layout.adapter_books_layout, parent, false);

        MyViewHolder vHolder = new MyViewHolder(v);

        return vHolder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        holder.tv_title.setText(mData.get(position).getTitle());
        holder.tv_author.setText(mData.get(position).getAuthor());
        holder.tv_location.setText(mData.get(position).getLocation());

        String bookID = mData.get(position).getBook_id();
        Log.d("bookid", "I'm trying to get this book img: "+bookID);

        if(bookID != null) {
            StorageReference storageReference = mStorageRef.child("bookCovers").child(bookID);
            /*Glide.with(mContext)
                    .using(new FirebaseImageLoader())
                    .load(storageReference)
                    .into(holder.book_img);*/
            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    String imageURL = uri.toString();
                    Glide.with(mContext).load(imageURL).into(holder.book_img);
                }
            });
        }else{
            holder.book_img.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_book_cover));
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{

        private TextView tv_title;
        private TextView tv_author;
        private TextView tv_location;
        private ImageView book_img;


        public MyViewHolder(View itemView){
            super(itemView);

            tv_title = (TextView) itemView.findViewById(R.id.book_title);
            tv_author = (TextView) itemView.findViewById(R.id.book_author);
            tv_location = (TextView) itemView.findViewById(R.id.book_location);
            book_img = (ImageView) itemView.findViewById(R.id.book_img);
        }
    }

    public String getLastItemId() {
        Log.d("booksfragment", "I've found "+mData.size()+" items");

        return mData.get(mData.size() - 1).getBook_id();
    }

    public void addAll(List<Book> newBooks) {
        int initialSize = mData.size();
        mData.addAll(newBooks);
        notifyItemRangeInserted(initialSize, newBooks.size());
    }

}
