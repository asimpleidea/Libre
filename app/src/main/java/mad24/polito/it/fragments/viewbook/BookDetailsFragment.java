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
import android.widget.LinearLayout;
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
import org.w3c.dom.Text;
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
    private boolean AlreadyQueried = false;

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

        if (getArguments() != null)
        {
            Data = new Gson().fromJson(getArguments().getString(BUNDLE_KEY), Book.class);
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
                        AlreadyQueried = true;
                        parseXML(response);
                        injectData();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        AlreadyQueried = true;
                        Log.d("VIEWBOOK", "on Error Response");
                        injectData();
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
                }

                //  Publisher
                if (node.getNodeName().equalsIgnoreCase("publisher"))
                {
                    Goodreads.setPublisher(node.getTextContent());
                    ++done;
                }

                //  Language code
                if (node.getNodeName().equalsIgnoreCase("language_code"))
                {
                    Goodreads.setLanguage(node.getTextContent());
                    ++done;
                }

                //  Description
                if (node.getNodeName().equalsIgnoreCase("description"))
                {
                    Goodreads.setDescription(node.getTextContent());
                    ++done;
                }

                //  Average rating
                if (node.getNodeName().equalsIgnoreCase("average_rating"))
                {
                    Goodreads.setAvgRating(node.getTextContent());
                    ++done;
                }

                //  Num pages
                if (node.getNodeName().equalsIgnoreCase("num_pages"))
                {
                    Goodreads.setPagesCount(Integer.valueOf(node.getTextContent()));
                    ++done;
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
                if(node.getNodeName().equalsIgnoreCase("name"))
                {
                    Goodreads.setAuthorName(node.getTextContent());
                    Log.d("VIEWBOOK", "AUthore name: " + Goodreads.getAuthorName());
                    ++fields;
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

    private void injectData()
    {
        // Set the title
        ((TextView) RootView.findViewById(R.id.bookTitle)).setText(Data.getTitle());

        //  Set the author
        //  NOTE: we prefer the goodreads author version because the user might misspell it
        String author = Goodreads != null ? Goodreads.getAuthorName() : Data.getAuthor();
        ((TextView) RootView.findViewById(R.id.bookAuthor)).setText(author);

        //  Set the description
        if(Goodreads != null)  ((TextView) RootView.findViewById(R.id.bookDescription)).setText(Goodreads.getDescription());

        //  Set the year
        String year = Goodreads != null ? Integer.toString(Goodreads.getPublicationYear()) : Data.getEditionYear();
        if(year.length() > 0)
        {
            TextView t = RootView.findViewById(R.id.bookPublicationYear);
            t.setText(year);
            ((LinearLayout) t.getParent()).setVisibility(View.VISIBLE);
        }

        //  Set the publisher
        String publisher = Goodreads != null ? Goodreads.getPublisher() : Data.getPublisher();
        if(publisher.length() > 0)
        {
            TextView t = RootView.findViewById(R.id.bookPublisher);
            t.setText(publisher);
            ((LinearLayout) t.getParent()).setVisibility(View.VISIBLE);
        }

        //------------------------------------------
        //  From Goodreads only
        //------------------------------------------

        if(Goodreads != null)
        {
            //  Set the language
            TextView t = RootView.findViewById(R.id.bookLanguage);
            t.setText(Goodreads.getLanguage());
            ((LinearLayout) t.getParent()).setVisibility(View.VISIBLE);

            //  Average rating
            t = RootView.findViewById(R.id.ratingMark);
            t.setText(Goodreads.getAvgRating());
            ((MaterialRatingBar) RootView.findViewById(R.id.ratingStars)).setRating(Float.valueOf(Goodreads.getAvgRating()));
            ((LinearLayout) t.getParent()).setVisibility(View.VISIBLE);

            //  Pages count
            t = RootView.findViewById(R.id.bookPages);
            t.setText(String.format(getResources().getString(R.string.book_pages), Goodreads.getPagesCount()));
            ((LinearLayout) t.getParent()).setVisibility(View.VISIBLE);

            //  Ratings count
            t = RootView.findViewById(R.id.ratingCount);
            t.setText(String.format(getResources().getString(R.string.book_ratings_count), Goodreads.getRatingsCount()));

            try
            {
                //  VERY WEIRD! Glide doesn't like it if it is not wrapped in a url!
                URL u = new URL(Goodreads.getAuthorImageUrl());
                CircleImageView c = RootView.findViewById(R.id.authorImage);
                Glide.with(getContext()).load(u.toString()).into(c);
                c.setVisibility(View.VISIBLE);
            }
            catch (MalformedURLException e)
            {
                //  Just don't do anything....
                //e.printStackTrace();
            }
        }

        //  Data loaded!
        //  TODO: raise finished event!
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        RootView = inflater.inflate(R.layout.fragment_book_details, container, false);

        if(Goodreads == null)
        {
            if(!AlreadyQueried) grabDataFromGoodreads();
            else injectData();
        }
        else injectData();

        return RootView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume()
    {
        super.onResume();
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
