package mad24.polito.it.fragments;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mad24.polito.it.EditProfileActivity;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {


    public ProfileFragment() {
        // Required empty public constructor
    }

    ImageView editImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    private TextView name;
    private TextView phone;
    private TextView mail;
    private TextView bio;
    private TextView city;

    private String[] genresList;
    private LinearLayout genres;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(mad24.polito.it.R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //button to edit profile
        editImage = (ImageView) getView().findViewById(mad24.polito.it.R.id.imageEdit);

        //image profile
        imageProfile = (de.hdodenhof.circleimageview.CircleImageView) getView().findViewById(mad24.polito.it.R.id.showImageProfile);

        //get edit fields
        name = (TextView) getView().findViewById(mad24.polito.it.R.id.showName);
        phone = (TextView) getView().findViewById(mad24.polito.it.R.id.showPhone);
        mail = (TextView) getView().findViewById(mad24.polito.it.R.id.showMail);
        bio = (TextView) getView().findViewById(mad24.polito.it.R.id.showBio);
        city = (TextView) getView().findViewById(mad24.polito.it.R.id.showCity);

        genres = (LinearLayout) getView().findViewById(mad24.polito.it.R.id.show_favourite_genres_list);
        genresList = getResources().getStringArray(mad24.polito.it.R.array.genres);

        //listener onClick for editing
        editImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), EditProfileActivity.class);
                startActivity(intent);
            }
        });

    }


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("state", "OnResume - show");
        //get preferences
        prefs = getActivity().getSharedPreferences("profile", MODE_PRIVATE);

        //get name if already inserted
        String str = prefs.getString("profileName", null);
        if (str != null)
            name.setText(str);

        str = prefs.getString("profilePhone", null);
        if(str != null)
            phone.setText(str);

        //get mail if already inserted
        str = prefs.getString("profileMail", null);
        if (str != null)
            mail.setText(str);

        //get bio if already inserted
        str = prefs.getString("profileBio", null);
        if (str != null)
            bio.setText(str);

        //get city if already inserted
        str = prefs.getString("profileCity", null);
        if (str != null)
            city.setText(str);

        //get image profile if already inserted
        str = prefs.getString("profileImage", null);
        Log.i("state", "show: "+str);
        if (str != null) {
            imageProfile.setImageURI(Uri.fromFile(new File(str)) );
        } else {
            //default image
            Drawable d = getResources().getDrawable(mad24.polito.it.R.drawable.unknown_user);
            imageProfile.setImageDrawable(d);
        }

        //get saved selectedItems
        str = prefs.getString("profileGenres", null);
        genres.removeAllViews();

        if(str != null && !str.isEmpty()) {
            String[] strArray = str.split(",");

            for(int i = 0; i < strArray.length; i++) {
                genres.addView(BuildGenreLayout(genresList[Integer.parseInt(strArray[i])] ) );
            }
        }

    }

    private TextView BuildGenreLayout(final String name) {
        TextView genre = new TextView(getActivity().getApplicationContext());
        genre.setText(name);
        genre.setTextSize(this.getResources().getDimension(mad24.polito.it.R.dimen.genre_item));
        genre.setTextColor(this.getResources().getColor(mad24.polito.it.R.color.black));

        return genre;
    }

}
