package mad24.polito.it.search;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import mad24.polito.it.R;
import mad24.polito.it.fragments.SearchFragment;

public class RecyclerViewAdapterGenre extends RecyclerView.Adapter<RecyclerViewAdapterGenre.MyViewHolder> {

    private Context mContext ;
    private List<Integer> mData;
    private String[] genresList;
    private SearchFragment searchFragment;


    public RecyclerViewAdapterGenre(Context mContext, List<Integer> mData, SearchFragment searchFragment) {
        this.mContext = mContext;
        this.mData = mData;
        this.genresList = mContext. getResources().getStringArray(R.array.genres);
        this.searchFragment = searchFragment;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view ;
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        view = mInflater.inflate(R.layout.cardview_genre, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        switch (mData.get(position)) {
            case 0:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_action_96) );
                break;
            case 1:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_paint_palette_96) );
                break;
            case 2:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_resume_96) );
                break;
            case 3:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_kitchen_96) );
                break;
            case 4:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_drama_96) );
                break;
            case 5:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_fantasy_96) );
                break;
            case 6:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_compass_96) );
                break;
            case 7:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_colosseum_96) );
                break;
            case 8:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_horror_96) );
                break;
            case 9:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_boy_96) );
                break;
            case 10:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_sci_fi_96) );
                break;
            case 11:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_question_mark_96) );
                break;
            case 12:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_book_96) );
                break;
            case 13:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_test_tube_96) );
                break;
            case 14:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_robot_2_96) );
                break;
            case 15:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_student_male_96) );
                break;
            case 16:
                holder.genre_thumbnail.setImageDrawable(holder.itemView.getContext().getResources().getDrawable(R.drawable.icons8_beach_96) );
                break;
            default:
                break;
        }

        holder.genre_title.setText(genresList[mData.get(position)]);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFragment.selectGenre(mData.get(position));
            }
        });
    }

    public void addAll(List<Integer> newGenres) {
        int initialSize = mData.size();
        mData.addAll(newGenres);
        notifyItemRangeInserted(initialSize, newGenres.size());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView genre_title;
        ImageView genre_thumbnail;
        CardView cardView;

        public MyViewHolder(View itemView) {
            super(itemView);

            genre_title = (TextView) itemView.findViewById(R.id.genre_genre) ;
            genre_thumbnail = (ImageView) itemView.findViewById(R.id.genre_img);
            cardView = (CardView) itemView.findViewById(R.id.genre_cardview);
        }
    }

}
