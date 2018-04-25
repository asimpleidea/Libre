package mad24.polito.it.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import mad24.polito.it.EditProfileActivity;
import mad24.polito.it.R;
import mad24.polito.it.registrationmail.LoginActivity;
import mad24.polito.it.models.UserMail;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    private int EDIT_PROFILE = 1;

    ImageButton editImage;

    de.hdodenhof.circleimageview.CircleImageView imageProfile;

    private TextView name;
    private TextView phone;
    private TextView mail;
    private TextView bio;
    private TextView city;

    private String[] genresList;
    private LinearLayout genres;

    private Button buttonLogoutProfile;

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    FirebaseUser userAuth;
    UserMail user;
    FirebaseDatabase database;

    Bitmap profileImageBitmap = null;

    Boolean semaphoreData = false;
    Boolean semaphoreImage = false;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(mad24.polito.it.R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //get user
        userAuth = FirebaseAuth.getInstance().getCurrentUser();

        //check if logged, if not go to login activity
        if (userAuth == null) {
            Intent i = new Intent(getActivity().getApplicationContext(), LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);

            FragmentManager fm = getActivity().getSupportFragmentManager();
            for(int j = 0; j < fm.getBackStackEntryCount(); ++j)
                fm.popBackStack();

        }

        //get shared preferences
        prefs = getActivity().getSharedPreferences("profile", MODE_PRIVATE);
        editor = prefs.edit();

        //button to edit profile
        editImage = (ImageButton) getView().findViewById(mad24.polito.it.R.id.showprofile_imageEdit);

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

        //get data from Firebase Database
        database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");

        usersRef.child(userAuth.getUid() ).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //get User object
                user = snapshot.getValue(UserMail.class);

                if(user == null)
                    return;

                //set fields
                name.setText(user.getName());
                phone.setText(user.getPhone());
                mail.setText(user.getEmail());
                bio.setText(user.getBio());
                city.setText(user.getCity());

                //set favourite genres
                genres.removeAllViews();
                if(user.getGenres() == null)
                    genres.addView(BuildGenreLayout(getResources().getString(R.string.noFavouriteGenreProfile) ) );
                else {
                    for (Integer genreIndex : user.getGenres()) {
                        if(getContext() != null) {
                            genres.addView(BuildGenreLayout(genresList[genreIndex]));
                        }
                    }
                }

                synchronized (semaphoreData) {
                    semaphoreData = true;
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                showDialog(getResources().getString(R.string.invalidName),
                        getResources().getString(R.string.editprofile_insertValidName));
            }
        });

        if(profileImageBitmap == null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + userAuth.getUid() + ".jpg");
            try {
                final File localFile = File.createTempFile("Images", "bmp");
                ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        profileImageBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        imageProfile.setImageBitmap(profileImageBitmap);

                        synchronized (semaphoreImage) {
                            semaphoreImage = true;
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageProfile.setImageDrawable(getResources().getDrawable(R.drawable.unknown_user));
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            imageProfile.setImageBitmap(profileImageBitmap);
        }

        //listener onClick for editing
        editImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if data are loaded
                synchronized (semaphoreData) {
                    if(!semaphoreData) {
                        showDialog(getResources().getString(R.string.showprofile_dataNotLoaded),
                                getResources().getString(R.string.showprofile_waitData));
                        return;
                    }
                }

                //check if profile image is loaded
                synchronized (semaphoreImage) {
                    if(!semaphoreImage) {
                        showDialog(getResources().getString(R.string.showprofile_dataNotLoaded),
                                getResources().getString(R.string.showprofile_waitData));
                        return;
                    }
                }

                //convert in JSON the User object
                Gson gson = new Gson();
                String userJson = gson.toJson(user);

                //save on sharedPreferences the User object
                editor.putString("user", userJson);

                //save profile image if not the standard one
                if(profileImageBitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    profileImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] b = baos.toByteArray();
                    String encoded = Base64.encodeToString(b, Base64.DEFAULT);
                    editor.putString("profileImage", encoded);
                } else {
                    editor.putString("profileImage", "unknown");
                }

                editor.commit();

                Intent intent = new Intent(getActivity().getApplicationContext(), EditProfileActivity.class);
                startActivityForResult(intent, EDIT_PROFILE);
            }
        });

        //listener onClick for logout
        buttonLogoutProfile = (Button) getView().findViewById(mad24.polito.it.R.id.buttonLogoutProfile);
        buttonLogoutProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("state", "Signout");
                FirebaseAuth.getInstance().signOut();

                Intent i = new Intent(getActivity().getApplicationContext(), LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

                FragmentManager fm = getActivity().getSupportFragmentManager();
                for(int j = 0; j < fm.getBackStackEntryCount(); ++j)
                    fm.popBackStack();

            }
        });

    }

    private TextView BuildGenreLayout(final String name) {
        TextView genre = new TextView(getContext());
        genre.setText(name);
        genre.setTextSize(this.getResources().getDimension(mad24.polito.it.R.dimen.genre_item));
        genre.setTextColor(this.getResources().getColor(mad24.polito.it.R.color.black));

        return genre;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("state", "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE && resultCode == getActivity().RESULT_OK) {
            //if something has been modified
            if (data.getBooleanExtra("modified", false))
                Toast.makeText(getContext().getApplicationContext(), mad24.polito.it.R.string.saved, Toast.LENGTH_SHORT).show();

            if (data.getBooleanExtra("imageModified", false)) {
                //get profile image
                String encoded = data.getStringExtra("profileImage");
                if(encoded != null) {
                    byte[] imageAsBytes = Base64.decode(encoded.getBytes(), Base64.DEFAULT);
                    profileImageBitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
                    imageProfile.setImageBitmap(profileImageBitmap);
                }
            }
        }

    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
}
