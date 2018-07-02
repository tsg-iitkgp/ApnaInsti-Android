package app.insti.fragment;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.util.List;

import app.insti.ActivityBuffer;
import app.insti.Constants;
import app.insti.ItemClickListener;
import app.insti.MainActivity;
import app.insti.R;
import app.insti.adapter.FeedAdapter;
import app.insti.data.AppDatabase;
import app.insti.data.Event;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyEventsFragment extends BaseFragment {

    private RecyclerView myEventsFeedRecyclerView;
    private SwipeRefreshLayout myEventsFeedSwipeRefreshLayout;
    private AppDatabase appDatabase;
    private FloatingActionButton fab;

    public MyEventsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_events, container, false);

        fab = (FloatingActionButton) view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddEventFragment addEventFragment = new AddEventFragment();
                addEventFragment.setArguments(getArguments());
                FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.replace(R.id.relative_layout, addEventFragment);
                ft.addToBackStack("addEvent");
                ft.commit();
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (((MainActivity)getActivity()).createEventAccess()) {
            fab.setVisibility(View.VISIBLE);
        }

        appDatabase = AppDatabase.getAppDatabase(getContext());
        new showEvents().execute();

        myEventsFeedSwipeRefreshLayout = getActivity().findViewById(R.id.my_events_feed_swipe_refresh_layout);
        myEventsFeedSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateOnRefresh();
                myEventsFeedSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void updateOnRefresh() {

        new showEvents().execute();

    }

    private void displayEvents(final List<Event> events) {
        final FeedAdapter feedAdapter = new FeedAdapter(events, new ItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                String eventJson = new Gson().toJson(events.get(position));
                Bundle bundle = getArguments();
                if (bundle == null)
                    bundle = new Bundle();
                bundle.putString(Constants.EVENT_JSON, eventJson);
                EventFragment eventFragment = new EventFragment();
                eventFragment.setArguments(bundle);
                FragmentManager manager = getActivity().getSupportFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.framelayout_for_fragment, eventFragment, eventFragment.getTag());
                transaction.addToBackStack(eventFragment.getTag()).commit();
            }
        });
        getActivityBuffer().safely(new ActivityBuffer.IRunnable() {
            @Override
            public void run(Activity pActivity) {
                try {
                    myEventsFeedRecyclerView = getActivity().findViewById(R.id.my_events_feed_recycler_view);
                    myEventsFeedRecyclerView.setAdapter(feedAdapter);
                    myEventsFeedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class showEvents extends AsyncTask<String, Void, List<Event>> {

        @Override
        protected List<Event> doInBackground(String... events) {

            List<Event> temp = appDatabase.dbDao().getAllEvents();
            List<Event> eventsfollowing = appDatabase.dbDao().getAllEvents();
            eventsfollowing.clear();
            int k = temp.size();
            for (int i = 0; i < k; i++) {
                if (temp.get(i).getEventUserUes() != 0) eventsfollowing.add(temp.get(i));
            }
            return eventsfollowing;
        }

        protected void onPostExecute(List<Event> result) {
            displayEvents(result);
        }
    }
}