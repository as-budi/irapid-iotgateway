package com.example.irapid_gateway;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ProximityManager proximityManager;
    public Gateway gateway;
    public Beacon beacon;
    public GatewayLocation gatewayLocation;
//    public GetAPIData getAPIData;
    public ArrayList<IBeaconDevice> filteredBeacon = new ArrayList<>();
    public ArrayList<String> targetUID = new ArrayList<>();

    private static final String KEYSTORE_PASSWORD = "password";
    private static final String CERTIFICATE_ID = "default";
    private static final String KEYSTORE_NAME = "iot_keystore";

//    private String apiURL = "https://cjue3pzetf.execute-api.ap-southeast-1.amazonaws.com/default/gateway/getbeaconlist";
//    private static String CLIENT_ID = "3b158074-0cde-4280-a9f9-a8b50bd402ca";
    private String CLIENT_ID = " ";
    private static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;

    private static final String ENDPOINT = "a2wm19da1f8kek-ats.iot.ap-southeast-1.amazonaws.com";
    private static final String COGNITO_POOL_ID = "ap-southeast-1:e8691940-b5e4-4cb0-90f1-8b2f519e7653";
    private static final String POLICY_NAME = "irapid_iotpolicy";
    private static final String TOPIC = "irapid_ble";

//    private static final String ENDPOINT = "a35pebv0rcc7jc-ats.iot.us-east-1.amazonaws.com";
//    private static final String COGNITO_POOL_ID = "us-east-1:63455c21-5682-4416-a79c-08092ab02eaf";
//    private static final Regions MY_REGION = Regions.US_EAST_1;
//    private static final String POLICY_NAME = "IoT_Device_Connect";

    public static final int REQUEST_CODE_PERMISSIONS = 100;
    public static final String LOG_TAG = "iRapid";
    private static final String API_KEY = "KDmqsFvgFqoeKuPkgcFfelRlesWAEzhC";

    TextView dataMessage;
    TextView latitudeMessage;
    TextView longitudeMessage;
    private Button getLocationButton;
    private Button startScanningButton;
    private Button stopScanningButton;

    AWSIotMqttManager mqttManager = new AWSIotMqttManager(CLIENT_ID, ENDPOINT);
    AWSIotClient mIotAndroidClient;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    KeyStore clientKeyStore = null;
    String certificateId;
    CognitoCachingCredentialsProvider credentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeMessage = (TextView) findViewById(R.id.latitudeMessage);
        longitudeMessage = (TextView) findViewById(R.id.longtitudeMessage);
        dataMessage = (TextView) findViewById(R.id.dataMessage);

        CLIENT_ID = getID();

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(CLIENT_ID, ENDPOINT);
        subscribe();

        mqttManager.setKeepAlive(10);

        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic", "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath, keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest = new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult = mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG, "Cert ID: " + createKeysAndCertificateResult.getCertificateId() + " created.");

                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId, createKeysAndCertificateResult.getCertificatePem(), createKeysAndCertificateResult.getKeyPair().getPrivateKey(), keystorePath, keystoreName, keystorePassword);

                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId, keystorePath, keystoreName, keystorePassword);

                        AttachPrincipalPolicyRequest policyAttachRequest = new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult.getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception occurred when generating new private key and certificate.", e);
                    }
                }
            }).start();
        }

        connect();

        InitializeDependencies();
        checkPermissions();

        getLocationButton = findViewById(R.id.getLocationButton);
        getLocationButton.setOnClickListener(this::retrieveLocation);

        startScanningButton = findViewById(R.id.startScanningButton);
        startScanningButton.setOnClickListener(this::StartScan);

        stopScanningButton = findViewById(R.id.stopScanningButton);
        stopScanningButton.setOnClickListener(this::StopScan);

        proximityManager = ProximityManagerFactory.create(this);
        proximityManager.setIBeaconListener(createIBeaconListener());

//        recyclerView = findViewById(R.id.dataRv);
//        ArrayList<DataRvItem> DataRvItems = new ArrayList<>();
//        dataAdapter = new DataAdapter(DataRvItems);
//        recyclerView.setAdapter(dataAdapter);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
//        layoutManager.setReverseLayout(true);
//        layoutManager.setStackFromEnd(true);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(layoutManager);

    }

    private void retrieveLocation(View view) {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                String lat = Double.toString(latitude);
                String lng = Double.toString(longitude);
                latitudeMessage.setText(lat);
                longitudeMessage.setText(lng);
                gatewayLocation = new GatewayLocation(lat, lng);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {

            Log.i("GPS", "GPS is not enabled.");
        }
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
//                retrieveBeaconList();
//                getAPIData.execute();

                targetUID.add("b9407f30-f5f8-466e-aff9-25556b57fe6d");
                targetUID.add("e1f54e02-1e23-44e0-9c3d-512eb56adec9");

                for (int i = 0; i < targetUID.size(); i++) {
                    if (ibeacon.getProximityUUID().toString().equals(targetUID.get(i))) {
                        filteredBeacon.add(ibeacon);
                    }
                }

                for (int i = 0; i < filteredBeacon.size(); i++) {
                    beacon = new Beacon(filteredBeacon.get(i).getTimestamp(), filteredBeacon.get(i).getProximityUUID().toString(), filteredBeacon.get(i).getDistance(), filteredBeacon.get(i).getProximity(), filteredBeacon.get(i).getRssi(), filteredBeacon.get(i).getTxPower(), 0);
                }

                Gson gson = new Gson();
                String json = gson.toJson(beacon);

                gateway = new Gateway(CLIENT_ID);
                Gson gson2 = new Gson();
                String json2 = gson2.toJson(gateway);

                if (json.length() > 0) {
                    json = json.substring(0, json.length() - 1) + ",";
                }

                if (json2.length() > 0) {
                    json2 = json2.substring(1);
                }

                String merged = json.concat("").concat(json2);

                Log.i("Beacon", "IBeacon discovered: " + merged);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dataMessage.setText(merged);
//                        publishBeacon(merged);
                    }
                }, 5000);

            }

            private void getAPIData(String apiURL) {
            }
        };
    }

    private void InitializeDependencies() {
        KontaktSDK.initialize(API_KEY);
    }

    public void StartScan(View view) {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                if (proximityManager.isScanning()) {
                    Log.i("StartScan", "already scanning");
                    return;
                }
                proximityManager.startScanning();
                Log.i("Scan", "scanning");
            }
        });
    }

    public void StopScan(View view) {
        proximityManager.stopScanning();
        Log.i("StopScan", "stopping");
    }

    //Since Android Marshmallow starting a Bluetooth Low Energy scan requires permission from location group.
    private void checkPermissions() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
            //Permission not granted so we ask for it. Results are handled in onRequestPermissionsResult() callback.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_CODE_PERMISSIONS == requestCode) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
        }
    }

    public void connect() {
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }
    }

    public void subscribe() {
        try {
            mqttManager.subscribeToTopic(TOPIC, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String TOPIC, final byte[] data) {
                            Log.d(LOG_TAG, "topic = " + TOPIC);
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    public void publishBeacon(String beaconMessage) {
        try {
            mqttManager.publishString(beaconMessage, TOPIC, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public void retrieveBeaconList() {
        String apiEndpoint = "https://cjue3pzetf.execute-api.ap-southeast-1.amazonaws.com/default/gateway/getbeaconlist/";
        String apiKey = "O3henMjeBY2LI5EhKDt4V8L5BLQg1uWx85N5oF4k";
        String apiHeaders = "x-api-key:" + apiKey;
        String[] dataArray;

        try {
            URL url = new URL(apiEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", apiHeaders);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Gson gson = new Gson();
                dataArray = gson.fromJson(response.toString(), String[].class);

                for (int i = 0; i < dataArray.length; i++) {
                    Log.i("api gateway: ", dataArray[i]);
                }
            } else {
                Log.i("api gateway: ", "404");
            }

            connection.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

    }

    public String getID() {
        String androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        Log.i("Testing: ", androidID);
        return androidID;
    }

}