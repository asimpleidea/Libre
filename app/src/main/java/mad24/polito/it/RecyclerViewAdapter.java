package mad24.polito.it;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import mad24.polito.it.models.Book;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    Context mContext;
    List<Book> mData;

    public RecyclerViewAdapter(Context mContext, List<Book> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;
        v = LayoutInflater.from(mContext).inflate(R.layout.adapter_books_layout, parent, false);

        MyViewHolder vHolder = new MyViewHolder(v);

        return vHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        holder.tv_title.setText(mData.get(position).getTitle());
        holder.tv_author.setText(mData.get(position).getAuthor());
        holder.tv_location.setText(mData.get(position).getLocation());
        holder.book_img.setImageResource(mData.get(position).getPhoto());

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
