package mad24.polito.it;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mad24.polito.it.fragments.viewbook.ViewBookFragment;
import mad24.polito.it.models.Book;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private static final String FIREBASE_DATABASE_LOCATION_BOOKS = "books";
    private StorageReference mStorageRef;
    private StorageReference coverRef;
    private BooksActivity booksActivity = null;

    Context mContext;
    List<Book> mData;

    public RecyclerViewAdapter(Context mContext, List<Book> mData) {
        this.mContext = mContext;
        this.mData = mData;

        mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public void setBooksActivity(BooksActivity activity)
    {
        booksActivity = activity;
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

            StorageReference storageReference = mStorageRef.child("bookCovers").child(bookID+".jpg");

            /*Glide.with(mContext)
                    .using(new FirebaseImageLoader())
                    .load(storageReference)
                    .into(holder.book_img);*/

            storageReference.getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageURL = uri.toString();

                            //  Set the url of cover *INSIDE* the object (so we won't have to query it again later).
                            mData.get(holder.getAdapterPosition()).setBookImageLink(uri.toString());
                            Glide.with(mContext).load(imageURL).into(holder.book_img);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            holder.book_img.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_book_cover));
                        }
                    });
        }else{
            holder.book_img.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_book_cover));
        }

        //--------------------------------------
        //  Set item touch listener
        //--------------------------------------

        holder.itemView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //----------------------------------
                //  Init
                //----------------------------------

                //  TODO: check if passing object as JSON makes sense: what if the object is too large?
                //  Pass the book's data here, so we won't have to download everything again
                //  Doing as JSON, so I won't have to write too many .putString etc :D
                Bundle args = new Bundle();
                args.putString("book", new Gson().toJson(mData.get(holder.getAdapterPosition())));

                //----------------------------------
                //  Start the fragment
                //----------------------------------

                final ViewBookFragment b = new ViewBookFragment();
                b.setArguments(args);

                //----------------------------------
                //  Show me the book
                //----------------------------------

                booksActivity.setFragment(b, "ViewBook");
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void retreiveBooks(List<String> books_id) {

        Query query;

        for(String b : books_id){

            query = FirebaseDatabase.getInstance().getReference()
                    .child(FIREBASE_DATABASE_LOCATION_BOOKS)
                    .child(b);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Book book = dataSnapshot.getValue(Book.class);
                    /*for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                        books.add(bookSnapshot.getValue(Book.class));
                    }*/



                    //Log.d("booksfragment", "adding "+books.size()+" books");
                    mData.add(book);
                    notifyItemInserted(mData.size());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
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
