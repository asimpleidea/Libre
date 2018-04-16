package mad24.polito.it;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import  mad24.polito.it.R;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class BooksFragment extends Fragment {

    private ListView lv;
    private ArrayList<Book> books = new ArrayList<>();

    public BooksFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(mad24.polito.it.R.layout.fragment_book, container, false);


    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lv = (ListView) getActivity().findViewById(mad24.polito.it.R.id.book_list);

        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return books.size(); //#elements in the list
            }

            @Override
            public Object getItem(int position) {
                return null; //return object at position 'position'
            }

            @Override
            public long getItemId(int position) {
                return 0; // leave 0
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return null;
            }
        });
    }
}
