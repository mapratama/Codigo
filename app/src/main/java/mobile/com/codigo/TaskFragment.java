package mobile.com.codigo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import mobile.com.codigo.core.Alert;


public class TaskFragment extends Fragment {

    @BindView(R.id.recylerview) RecyclerView recyclerView;

    private List<AndroidAppProcess> processes;
//    private List<ActivityManager.RunningAppProcessInfo> processes;
    private PackageManager packageManager;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);
        ButterKnife.bind(this, view);

        processes = AndroidProcesses.getRunningAppProcesses();
//        processes = AndroidProcesses.getRunningAppProcessInfo(getActivity());
        packageManager = getActivity().getPackageManager();

        recyclerView.setAdapter(new AppAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (intent == null) {
            intent = new Intent();
        }
        super.startActivityForResult(intent, requestCode);
    }

    class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

        @Override
        public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new AppViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.app_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final AppViewHolder holder, final int position) {
            holder.packageName = processes.get(position).getPackageName();
            Log.e("####", "" + holder.packageName);

            try {
                ApplicationInfo app = packageManager.getApplicationInfo(holder.packageName, 0);
                holder.nameTextView.setText(packageManager.getApplicationLabel(app));
                holder.icon.setImageDrawable(packageManager.getApplicationIcon(app));
            } catch (PackageManager.NameNotFoundException e) {
                holder.nameTextView.setText("Error getting app name");
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return processes.size();
        }

        class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            @BindView(R.id.name) TextView nameTextView;
            @BindView(R.id.icon) ImageView icon;

            public String packageName;

            public AppViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
                view.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                try {
                    Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(packageName);
                    startActivity(intent);
                } catch (Exception e) {
                    Alert.alertDialog(getActivity(),
                            getActivity().getResources().getString(R.string.error_launch_intent_for_pavkage));
                }

            }
        }
    }
}

