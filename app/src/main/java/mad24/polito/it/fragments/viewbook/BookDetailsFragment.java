package mad24.polito.it.fragments.viewbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.hdodenhof.circleimageview.CircleImageView;
import mad24.polito.it.R;
import mad24.polito.it.models.Book;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BookDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BookDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BookDetailsFragment extends Fragment
{
    private Book Data = null;
    private final String BUNDLE_KEY = "book";
    private View RootView = null;
    private GoodreadsBook Goodreads = null;
    private final int GOODREADS_BOOK_FIELDS = 9;
    private final int GOODREADS_AUTHOR_FIELDS = 3;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public BookDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BookDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BookDetailsFragment newInstance(String param1, String param2) {
        BookDetailsFragment fragment = new BookDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("VIEWBOOK", "onCreate called");
        if (getArguments() != null)
        {
            Data = new Gson().fromJson(getArguments().getString(BUNDLE_KEY), Book.class);

            grabDataFromGoodreads();
        }
    }

    private void grabDataFromGoodreads()
    {
        //  If data is there then go get GoodReads!
        RequestQueue queue = Volley.newRequestQueue(getActivity().getApplicationContext());

        //  The url
        URL u = null;
        try
        {
            // UGLY hardcoded string. Gonna edit it later...
            u = new URL("https://www.goodreads.com/book/isbn?&key=rGvCtASV1hvUEwo1pldorA&isbn=" + /*Data.getIsbn()*/ "9781444720730");
        }
        catch(MalformedURLException m)
        {
            return;
        }

        //  Make the request
        StringRequest req = new StringRequest(Request.Method.GET, u.toString(),
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                       // Log.d("VIEWBOOK", response);
                        parseXML(response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d("VIEWBOOK", "on Error Response");
                    }
                }
        );

        queue.add(req);

    }

    private void parseXML(String xml)
    {
        //-------------------------------------
        //  Init
        //-------------------------------------

        InputStream inputXML = null;

        try
        {
            inputXML = new ByteArrayInputStream(xml.getBytes("UTF-8"));

            //  Build the document parser
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputXML);

            //  Does it include a book
            if(doc.getElementsByTagName("book").getLength() < 1)
            {
                inputXML.close();
                return;
            }

            Goodreads = new GoodreadsBook();

            //  Get the book node and the number of its elements
            NodeList bookNode = doc.getElementsByTagName("book").item(0).getChildNodes();
            int elems = bookNode.getLength(),
                    done = 0;

            //  Loop through the data
            for(int i = 0; i < elems && done < GOODREADS_BOOK_FIELDS; ++i)
            {
                //  Get the node
                Node node = bookNode.item(i);

                //  Title?
                if (node.getNodeName().equalsIgnoreCase("title"))
                {
                    Goodreads.setTitle(node.getTextContent());
                    ++done;
                }

                //  Image Url
                if (node.getNodeName().equalsIgnoreCase("image_url"))
                {
                    Goodreads.setImageUrl(node.getTextContent());
                    ++done;
                }

                //  Small Image url
                if (node.getNodeName().equalsIgnoreCase("small_image_url"))
                {
                    Goodreads.setSmallImageUrl(node.getTextContent());
                    ++done;
                }

                //  Publishing year
                if (node.getNodeName().equalsIgnoreCase("publication_year"))
                {
                    Goodreads.setPublicationYear(Integer.valueOf(node.getTextContent()));
                    ++done;
                    Log.d("VIEWBOOK", "value: " + String.valueOf(Data.getEditionYear()));
                    //  Set it if user did not
                    if(Data.getEditionYear() == null || (Data.getEditionYear() != null && Data.getEditionYear().length() < 1))
                    {
                        ((TextView) RootView.findViewById(R.id.bookPublicationYear)).setText(String.valueOf(Goodreads.getPublicationYear()));
                    }
                }

                //  Publisher
                if (node.getNodeName().equalsIgnoreCase("publisher"))
                {
                    Goodreads.setPublisher(node.getTextContent());
                    ++done;

                    if(Data.getPublisher() != null && Data.getPublisher().length() < 1) ((TextView) RootView.findViewById(R.id.bookPublisher)).setText(Goodreads.getPublisher());
                }

                //  Language code
                if (node.getNodeName().equalsIgnoreCase("language_code"))
                {
                    Goodreads.setLanguage(node.getTextContent());
                    ++done;

                    ((TextView) RootView.findViewById(R.id.bookLanguage)).setText(Goodreads.getLanguage());
                }

                //  Description
                if (node.getNodeName().equalsIgnoreCase("description"))
                {
                    Goodreads.setDescription(node.getTextContent());
                    ++done;

                    ((TextView) RootView.findViewById(R.id.bookDescription)).setText(Goodreads.getDescription());
                }

                //  Average rating
                if (node.getNodeName().equalsIgnoreCase("average_rating"))
                {
                    Goodreads.setAvgRating(node.getTextContent());
                    ++done;

                    ((TextView) RootView.findViewById(R.id.ratingMark)).setText(Goodreads.getAvgRating());
                    ((MaterialRatingBar) RootView.findViewById(R.id.ratingStars)).setRating(Float.valueOf(Goodreads.getAvgRating()));
                }

                //  Num pages
                if (node.getNodeName().equalsIgnoreCase("num_pages")) {
                    Goodreads.setPagesCount(Integer.valueOf(node.getTextContent()));
                    ++done;

                    ((TextView) RootView.findViewById(R.id.bookPages)).setText(String.format(getResources().getString(R.string.book_pages), Goodreads.getPagesCount()));
                }
            }

            NodeList work = doc.getElementsByTagName("work").item(0).getChildNodes();
            int workElems = work.getLength();

            for(int i = 0; i < workElems; ++i)
            {
                Node node = work.item(i);

                //  ratings count
                if(node.getNodeName().equalsIgnoreCase("ratings_count"))
                {
                    Goodreads.setRatingsCount(Integer.valueOf(node.getTextContent()));

                    ((TextView) RootView.findViewById(R.id.ratingCount)).setText(String.format(getResources().getString(R.string.book_ratings_count), Goodreads.getRatingsCount()));
                    break;
                }
            }

            NodeList author = doc.getElementsByTagName("author").item(0).getChildNodes();
            int authorElems = author.getLength(),
                    fields = 0;

            for(int i = 0; i < authorElems && fields < GOODREADS_AUTHOR_FIELDS; ++i)
            {
                Node node = author.item(i);

                //  Author name
                if(node.getNodeName().equalsIgnoreCase("author_name"))
                {
                    Goodreads.setAuthorName(node.getTextContent());
                    ++fields;

                    if(Data.getAuthor() != null && Data.getAuthor().length() < 1) ((TextView) RootView.findViewById(R.id.bookAuthor)).setText(Goodreads.getAuthorName());
                }

                //  Author image
                if(node.getNodeName().equalsIgnoreCase("image_url"))
                {
                    Goodreads.setAuthorImageUrl(node.getTextContent());
                    ++fields;

                }

                //  Author small image
                if(node.getNodeName().equalsIgnoreCase("small_image_url"))
                {
                    Goodreads.setAuthorSmallImageUrl(node.getTextContent());
                    ++fields;

                    //  VERY WEIRD! Glide doesn't like it if it is not wrapped in a url!
                    URL u = new URL(Goodreads.getAuthorImageUrl());
                    Glide.with(getContext()).load(u.toString()).into((ImageView) RootView.findViewById(R.id.authorImage));
                }
            }

        }
        catch (IOException e)
        {
            Log.d("VIEWBOOK", e.getLocalizedMessage());
        }
        catch (ParserConfigurationException e)
        {
            Log.d("VIEWBOOK", e.getLocalizedMessage());
        }
        catch (SAXException e)
        {
            Log.d("VIEWBOOK", e.getLocalizedMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_book_details, container, false);

        if(Goodreads != null) Log.d("VIEWBOOK", "IS !NULL");
        // Set the title
        ((TextView) RootView.findViewById(R.id.bookTitle)).setText(Data.getTitle());

        //  Set the author
        //  TODO: get it from GoodReads
        ((TextView) RootView.findViewById(R.id.bookAuthor)).setText(new String(getResources().getString(R.string.book_by) + " " + Data.getAuthor()));

        //  Set the description
        ((TextView) RootView.findViewById(R.id.bookDescription)).setText("CACCA");

        //  Set the year
        ((TextView) RootView.findViewById(R.id.bookPublicationYear)).setText(Data.getEditionYear());

        //  Set the publisher
        ((TextView) RootView.findViewById(R.id.bookPublisher)).setText(Data.getPublisher());
        return RootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        Log.d("VIEWBOOK", "save instance");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(Goodreads != null) Log.d("VIEWBOOK", "resumed with goodreads");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
        Log.d("VIEWBOOK", "detached");
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
