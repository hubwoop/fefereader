package de.timbolender.fefereader.ui;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.timbolender.fefereader.R;
import de.timbolender.fefereader.service.UpdateService;
import de.timbolender.fefereader.viewmodel.PostListViewModel;
import de.timbolender.fefereader.viewmodel.PostViewModel;

/**
 * Base activity class featuring a list of posts with default actions.
 */
public abstract class PostListActivity extends AppCompatActivity implements PostPagedAdapter.OnPostSelectedListener  {
    static final String TAG = PostListActivity.class.getSimpleName();

    PostListViewModel vm;

    PostPagedAdapter postAdapter;
    boolean shouldPerformUpdate;

    RecyclerView postList;
    SwipeRefreshLayout refreshLayout;

    BroadcastReceiver updateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prepare ui
        setContentView(R.layout.activity_main);
        vm = ViewModelProviders.of(this).get(PostListViewModel.class);
        postList = findViewById(R.id.post_list);
        refreshLayout = findViewById(R.id.refresh_layout);

        // Prepare list view
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        postList.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        postList.addItemDecoration(dividerItemDecoration);

        // Fill content
        postAdapter = new PostPagedAdapter(this);
        getPostPagedList().observe(this, postAdapter::submitList);
        postList.setAdapter(postAdapter);

        // Handle swipe update gesture
        if(isRefreshGestureEnabled()) {
            refreshLayout.setOnRefreshListener(() -> UpdateService.startManualUpdate(PostListActivity.this));
            refreshLayout.setColorSchemeResources(R.color.colorAccent);
        }

        // Create receiver for updates
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received content update notification");
                refreshLayout.setRefreshing(false);
                abortBroadcast();
            }
        };

        // Trigger update if desired
        shouldPerformUpdate = isUpdateOnStartEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Drop all user notifications
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // Register broadcast receiver for notifications
        IntentFilter updateFilter = new IntentFilter(UpdateService.BROADCAST_UPDATE_FINISHED);
        updateFilter.setPriority(UpdateService.BROADCAST_PRIORITY_UI);
        registerReceiver(updateReceiver, updateFilter);
        IntentFilter skippedFilter = new IntentFilter(UpdateService.BROADCAST_UPDATE_SKIPPED);
        registerReceiver(updateReceiver, skippedFilter);

        // Trigger update if desired
        refreshLayout.setRefreshing(false);
        if(shouldPerformUpdate) {
            requestUpdate();
            shouldPerformUpdate = false;
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(updateReceiver);

        super.onPause();
    }

    //
    // Behavior determining methods
    //

    abstract LiveData<PagedList<PostViewModel>> getPostPagedList();

    abstract boolean isUpdateOnStartEnabled();

    abstract boolean isRefreshGestureEnabled();

    //
    // General utility functions
    //

    void requestUpdate() {
        refreshLayout.setRefreshing(true);
        UpdateService.startManualUpdate(this);
    }

    //
    // Post entry handling
    //

    @Override
    public void onPostSelected(@NotNull String postId) {
        vm.markPostAsRead(postId);
        Intent intent = DetailsActivity.createShowPostIntent(this, postId);
        startActivity(intent);
    }

    @Override
    public boolean onPostLongPressed(@NotNull String postId) {
        vm.togglePostBookmark(postId);
        return true;
    }
}
