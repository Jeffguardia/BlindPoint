package com.blindpoint.blindpoint;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.kml.KmlLayer;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;

public class ActivityMapa extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private GoogleMap mMap;
    protected GoogleApiClient googleApiClient;
    protected Location location, locationDest;
    protected LocationRequest locationRequest;
    private LatLng lolizacaoAtual;
    public KmlLayer kmlLayer;
    private LatLng dest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        criarUserGoogleApi();
        if (lolizacaoAtual != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lolizacaoAtual, 15));
        }
        // Leitura do arquivo KML
        try {
            kmlLayer = new KmlLayer(mMap, R.raw.itinerarioonibus, this);
            kmlLayer.addLayerToMap();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Conectando com GoogleAPiCliente
    protected synchronized void criarUserGoogleApi() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            comecarLocationUpdate();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            pararLocationUpdate();
            googleApiClient.disconnect();
        }
    }

    /* Quando se conectar ao Google Api Client começar a rodar o metodo de comecarLocationUpdate();
    que inicializao serviço de atualização do nosso ponto.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(getBaseContext(), "Passou pela permissão !", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            Toast.makeText(getBaseContext(), "Não está vazio", Toast.LENGTH_SHORT).show();
            System.out.println(location.getLatitude());
            System.out.println(location.getLongitude());
        } else {
            Toast.makeText(getBaseContext(), "Está vazio!", Toast.LENGTH_SHORT).show();
        }

        comecarLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getBaseContext(), "Conexão suspendida!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getBaseContext(), "Falha na conexão!", Toast.LENGTH_SHORT).show();
    }

    /*Método responsável por manter o periodo de atulização do posicionamento e o tipo de precisão
     do GPS */

    public void requestLocation() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void comecarLocationUpdate() {
        requestLocation();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, ActivityMapa.this);
    }

    //Método responsável por remover o updateLocation
    public void pararLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, ActivityMapa.this);
    }

    /*Método recebe um parâmetro Location com latitude e longitude e atualiza o valor da nossa localização
      atual, atualiza a câmera e verifica sempre se estamos próximo do ponto final.
     */

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(getBaseContext(), "Localização Atualizada!", Toast.LENGTH_SHORT).show();

        lolizacaoAtual = new LatLng(location.getLatitude(), location.getLongitude());

        CameraPosition cameraPosition = new CameraPosition.Builder().target(lolizacaoAtual).zoom(16).bearing(90).tilt(30).build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (locationDest != null) {
            float distance = location.distanceTo(locationDest);
            System.out.println(distance);

            if (distance < 120.000) {
                Toast.makeText(this, "Você está chegando ao seu ponto !", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Método Responsavél por setar o ponto final
    public void gerarPontoFinal() {
        dest = new LatLng(-23.934116, -46.354068);
        locationDest = new Location("Destino");
        locationDest.setLatitude(-23.934116);
        locationDest.setLongitude(-46.354068);
        mMap.addMarker(new MarkerOptions().title("Ponto Final 184").snippet("Apenas um teste").position(dest));
    }
}
