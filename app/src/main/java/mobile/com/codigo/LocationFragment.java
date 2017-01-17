package mobile.com.codigo;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mobile.com.codigo.core.Alert;
import mobile.com.codigo.core.LoadingDialog;


public class LocationFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    @BindView(R.id.parent_layout) RelativeLayout parentLayout;

    private MapFragment mapFragment;
    private GoogleMap googleMap;
    private GoogleApiClient apiClient;
    private LatLng location, defaultLocation = new LatLng(-6.228860, 106.819804);
    private final static int PLAY_SERVICES_REQUEST = 123, LOCATION_PERMISSION_REQUEST = 345;
    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) parent.removeView(view);
        }

        try {
            view = inflater.inflate(R.layout.fragment_location, container, false);
        } catch (InflateException e) {}

        ButterKnife.bind(this, view);

        try {
            MapsInitializer.initialize(getActivity());
        } catch (Exception e) {
            Alert.alertDialog(getActivity(), getResources().getString(R.string.error_load_map));
            return view;
        }

        return view;
    }

    private void displayCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions =  {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(getActivity(), permissions, LOCATION_PERMISSION_REQUEST);
        }

        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);

        if (currentLocation != null) {
            location = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            setLocation(location);
        }
        else {
            location = defaultLocation;
            Snackbar.make(parentLayout, getResources().getString(R.string.error_get_current_location),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void setLocation(LatLng location) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 17);
        googleMap.animateCamera(cameraUpdate);
    }

    private boolean playServicesIsAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getActivity());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result))
                googleAPI.getErrorDialog(getActivity(), result, PLAY_SERVICES_REQUEST).show();
            return false;
        }

        return true;
    }

    private String getAddressByLatLng(LatLng location) {
        Geocoder gcd = new Geocoder(getActivity(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(location.latitude, location.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses.size() > 0) return addresses.get(0).getThoroughfare();
        else return  "Unknown";
    }

    private void sendLocation(final String fcmToken) {
        final LatLng center = googleMap.getCameraPosition().target;
        final String address = getAddressByLatLng(center);

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... urls) {
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("https://fcm.googleapis.com/fcm/send");

                    JSONObject data = new JSONObject();
                    try {
                        JSONObject notification = new JSONObject();
                        notification.accumulate("title", "I am in " + address);
                        notification.accumulate("body", address + " in " + center.toString());

                        data.accumulate("notification", notification);
                        data.accumulate("to", fcmToken);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    httpPost.setEntity(new StringEntity(data.toString()));
                    httpPost.setHeader("Authorization", "key=" + getResources().getString(R.string.fcm_server_key));
                    httpPost.setHeader("Content-type", "application/json");

                    HttpResponse response = httpClient.execute(httpPost);
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) return getResources().getString(R.string.successfuly_send_location);

                    return response.getStatusLine().toString();

                } catch (ClientProtocolException e) {
                    return getResources().getString(R.string.error_send_location);
                } catch (IOException e) {
                    return getResources().getString(R.string.error_send_location);
                }
            }

            @Override
            protected void onPostExecute(String result) {
                Snackbar.make(parentLayout, result, Snackbar.LENGTH_LONG).show();
            }
        }.execute();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (playServicesIsAvailable()) {
            apiClient = new GoogleApiClient.Builder(getActivity())
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        else Alert.alertDialog(getActivity(), getResources().getString(R.string.error_google_play_services));

        mapFragment = (MapFragment) getActivity().getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mGoogleMap) {
                googleMap = mGoogleMap;
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 17));
                apiClient.connect();
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getActivity().getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                setLocation(place.getLatLng());
            }

            @Override
            public void onError(Status status) {
                Alert.alertDialog(getActivity(), getResources().getString(R.string.error_autocomplete_location));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        apiClient.disconnect();
        googleMap.clear();
    }

    @OnClick(R.id.send_location)
    public void sendLocationButtonOnClick() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        View dialogView = getActivity().getLayoutInflater().inflate(R.layout.input_fcm_layout, null);
        dialogBuilder.setView(dialogView);

        final EditText fcmTokenEditText = (EditText) dialogView.findViewById(R.id.fcm_token);

        dialogBuilder.setTitle("Send Location ");
        dialogBuilder.setMessage("Enter FCM Token recipient");
        dialogBuilder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendLocation(fcmTokenEditText.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", null);

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @OnClick(R.id.current_location)
    public void currentLocationButtonOnClick() {
        displayCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) displayCurrentLocation();
        else Alert.alertDialog(getActivity(), getResources().getString(R.string.ignore_location_permission));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayCurrentLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onLocationChanged(Location location) {}
}
