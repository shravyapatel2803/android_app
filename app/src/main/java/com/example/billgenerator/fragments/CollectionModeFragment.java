package com.example.billgenerator.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.DebtCustomerAdapter;
import com.example.billgenerator.adapters.LocationSummaryAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.DebtCustomerItem;
import com.example.billgenerator.models.LocationSummaryItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectionModeFragment extends Fragment {

    private static final double DEFAULT_DISTANCE_KM = 25.0;
    private static final double MIN_DISTANCE_KM = 1.0;
    private static final double MAX_DISTANCE_KM = 200.0;

    private final ArrayList<DebtCustomerItem> nearbyItems = new ArrayList<>();
    private final ArrayList<DebtCustomerItem> allDebtItems = new ArrayList<>();
    private final ArrayList<LocationSummaryItem> locationSummaryItems = new ArrayList<>();
    private final Map<Integer, Float> distanceByCustomerId = new HashMap<>();
    private final Map<String, double[]> geocodeCache = new HashMap<>();

    private databaseSystem dbHelper;
    private DebtCustomerAdapter adapter;
    private LocationSummaryAdapter locationAdapter;
    private TextView statusText;
    private TextView summaryText;
    private TextView emptyText;
    private TextView locationEmptyText;
    private View progressView;
    private View customerViewContainer;
    private View locationViewContainer;
    private MapView mapView;
    private TextInputEditText rangeInput;
    private double selectedRangeKm = DEFAULT_DISTANCE_KM;
    private boolean hasAutoTriggeredLocationSearch = false;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                boolean hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (granted || hasCoarse) {
                    detectAndLoadNearbyCustomers();
                } else {
                    statusText.setText("Location permission denied. Allow location to find nearby debt customers.");
                    Toast.makeText(requireContext(), "Location permission is required for Collection Mode", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collection_mode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize osmdroid configuration
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        dbHelper = new databaseSystem(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.collection_mode_recycler);
        RecyclerView locationRecycler = view.findViewById(R.id.location_summary_recycler);
        statusText = view.findViewById(R.id.collection_mode_status);
        summaryText = view.findViewById(R.id.collection_mode_summary);
        emptyText = view.findViewById(R.id.collection_mode_empty);
        locationEmptyText = view.findViewById(R.id.location_empty_text);
        progressView = view.findViewById(R.id.collection_mode_progress);
        rangeInput = view.findViewById(R.id.collection_mode_range_input);
        
        customerViewContainer = view.findViewById(R.id.customer_view_container);
        locationViewContainer = view.findViewById(R.id.location_view_container);
        mapView = view.findViewById(R.id.collection_map_view);
        TabLayout tabLayout = view.findViewById(R.id.collection_tabs);

        MaterialButton detectButton = view.findViewById(R.id.collection_mode_detect_button);
        MaterialButton refreshButton = view.findViewById(R.id.collection_mode_refresh_button);
        MaterialButton applyRangeButton = view.findViewById(R.id.collection_mode_apply_range_button);

        // Individual Customers Adapter
        adapter = new DebtCustomerAdapter(requireContext(), nearbyItems, this::openDebtHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Location Summary Adapter
        locationAdapter = new LocationSummaryAdapter(locationSummaryItems, this::onLocationClicked);
        locationRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        locationRecycler.setAdapter(locationAdapter);

        // Map Setup
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    customerViewContainer.setVisibility(View.VISIBLE);
                    locationViewContainer.setVisibility(View.GONE);
                } else {
                    customerViewContainer.setVisibility(View.GONE);
                    locationViewContainer.setVisibility(View.VISIBLE);
                    // Refresh map if needed
                    mapView.invalidate();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        detectButton.setOnClickListener(v -> ensureLocationAndLoad());
        refreshButton.setOnClickListener(v -> ensureLocationAndLoad());
        applyRangeButton.setOnClickListener(v -> {
            if (applyRangeFromInput()) {
                ensureLocationAndLoad();
            }
        });

        loadDebtCustomers();
        statusText.setText("Requesting location to find nearby debt customers...");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (!hasAutoTriggeredLocationSearch) {
            hasAutoTriggeredLocationSearch = true;
            ensureLocationAndLoad();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }

    private void ensureLocationAndLoad() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            detectAndLoadNearbyCustomers();
            return;
        }
        statusText.setText("Waiting for location permission...");
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void loadDebtCustomers() {
        allDebtItems.clear();
        Cursor cursor = null;
        try {
            cursor = dbHelper.fetchDebtCustomerDetails();
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow("customer_id");
                int nameCol = cursor.getColumnIndexOrThrow("customer_name");
                int phoneCol = cursor.getColumnIndexOrThrow("customer_phone");
                int villageCol = cursor.getColumnIndexOrThrow("customer_village");
                int totalDebtCol = cursor.getColumnIndexOrThrow("customer_total_debt");
                int activeDebtCol = cursor.getColumnIndexOrThrow("active_bill_debt");
                int dueDateCol = cursor.getColumnIndexOrThrow("nearest_due_date");
                int lastBillCol = cursor.getColumnIndexOrThrow("last_bill_date");

                do {
                    allDebtItems.add(new DebtCustomerItem(
                            cursor.getInt(idCol),
                            cursor.getString(nameCol),
                            cursor.getString(phoneCol),
                            cursor.getString(villageCol),
                            cursor.getDouble(totalDebtCol),
                            cursor.getDouble(activeDebtCol),
                            cursor.getString(dueDateCol),
                            cursor.getString(lastBillCol)
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        summaryText.setText(String.format(Locale.getDefault(), "%d debt customers loaded", allDebtItems.size()));
        emptyText.setVisibility(allDebtItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean applyRangeFromInput() {
        String value = rangeInput != null && rangeInput.getText() != null
                ? rangeInput.getText().toString().trim()
                : "";
        if (value.isEmpty()) {
            selectedRangeKm = DEFAULT_DISTANCE_KM;
            return true;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < MIN_DISTANCE_KM || parsed > MAX_DISTANCE_KM) {
                Toast.makeText(requireContext(), "Range must be between 1 and 200 km", Toast.LENGTH_SHORT).show();
                return false;
            }
            selectedRangeKm = parsed;
            return true;
        } catch (NumberFormatException ex) {
            Toast.makeText(requireContext(), "Invalid range value", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void detectAndLoadNearbyCustomers() {
        if (!applyRangeFromInput()) {
            return;
        }
        if (allDebtItems.isEmpty()) {
            loadDebtCustomers();
        }
        Location currentLocation = getBestLastKnownLocation();
        if (currentLocation == null) {
            statusText.setText("Could not detect current location. Turn on GPS/location and try again.");
            Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText(String.format(Locale.getDefault(), "Current location: %.5f, %.5f", currentLocation.getLatitude(), currentLocation.getLongitude()));
        progressView.setVisibility(View.VISIBLE);

        worker.execute(() -> {
            ArrayList<DebtCustomerItem> matched = new ArrayList<>();
            Map<Integer, Float> matchedDistance = new HashMap<>();
            
            Map<String, Integer> customerCountByVillage = new HashMap<>();
            Map<String, Double> totalDebtByVillage = new HashMap<>();
            Map<String, double[]> coordinatesByVillage = new HashMap<>();

            for (DebtCustomerItem item : allDebtItems) {
                String village = item.village == null ? "" : item.village.trim();
                if (village.isEmpty()) {
                    continue;
                }

                double[] latLon = getVillageLatLon(village, currentLocation);
                if (latLon == null) {
                    continue;
                }

                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        latLon[0], latLon[1],
                        results
                );
                float km = results[0] / 1000f;
                if (km <= selectedRangeKm) {
                    matched.add(item);
                    matchedDistance.put(item.customerId, km);
                    
                    // Aggregate for location view
                    customerCountByVillage.put(village, customerCountByVillage.getOrDefault(village, 0) + 1);
                    totalDebtByVillage.put(village, totalDebtByVillage.getOrDefault(village, 0.0) + item.totalDebt);
                    coordinatesByVillage.put(village, latLon);
                }
            }

            ArrayList<LocationSummaryItem> summaries = new ArrayList<>();
            for (String village : customerCountByVillage.keySet()) {
                double[] coords = coordinatesByVillage.get(village);
                summaries.add(new LocationSummaryItem(
                        village,
                        customerCountByVillage.get(village),
                        totalDebtByVillage.get(village),
                        coords[0],
                        coords[1]
                ));
            }
            
            // Sort summaries by customer count descending
            summaries.sort((a, b) -> Integer.compare(b.customerCount, a.customerCount));

            Collections.sort(matched, Comparator
                    .comparing((DebtCustomerItem a) -> matchedDistance.getOrDefault(a.customerId, Float.MAX_VALUE))
                    .thenComparing(a -> -Math.abs(a.totalDebt)));

            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                progressView.setVisibility(View.GONE);
                nearbyItems.clear();
                nearbyItems.addAll(withDistanceLabel(matched, matchedDistance));
                distanceByCustomerId.clear();
                distanceByCustomerId.putAll(matchedDistance);

                adapter.notifyDataSetChanged();
                summaryText.setText(String.format(Locale.getDefault(), "%d nearby customers within %.1f km", nearbyItems.size(), selectedRangeKm));
                emptyText.setVisibility(nearbyItems.isEmpty() ? View.VISIBLE : View.GONE);
                
                // Update Location Summary
                locationSummaryItems.clear();
                locationSummaryItems.addAll(summaries);
                locationAdapter.notifyDataSetChanged();
                locationEmptyText.setVisibility(locationSummaryItems.isEmpty() ? View.VISIBLE : View.GONE);
                
                updateMapMarkers(currentLocation);
            });
        });
    }

    private void updateMapMarkers(Location currentLocation) {
        if (mapView == null) return;
        
        mapView.getOverlays().clear();
        
        // Add current location marker
        Marker currentMarker = new Marker(mapView);
        currentMarker.setPosition(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        currentMarker.setTitle("You are here");
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(currentMarker);

        for (LocationSummaryItem summary : locationSummaryItems) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(summary.latitude, summary.longitude));
            marker.setTitle(summary.village + " (" + summary.customerCount + " customers)");
            marker.setSnippet(String.format(Locale.getDefault(), "Total Debt: Rs %.2f", summary.totalDebt));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }

        mapView.getController().animateTo(new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
        mapView.invalidate();
    }

    private void onLocationClicked(LocationSummaryItem item) {
        if (mapView != null) {
            mapView.getController().animateTo(new GeoPoint(item.latitude, item.longitude));
            mapView.getController().setZoom(15.0);
        }
    }

    private ArrayList<DebtCustomerItem> withDistanceLabel(List<DebtCustomerItem> source, Map<Integer, Float> distanceMap) {
        ArrayList<DebtCustomerItem> labeled = new ArrayList<>();
        for (DebtCustomerItem item : source) {
            float km = distanceMap.getOrDefault(item.customerId, -1f);
            String villageWithDistance = item.village;
            if (km >= 0) {
                villageWithDistance = (item.village == null ? "-" : item.village) + " | "
                        + String.format(Locale.getDefault(), "%.1f km away", km);
            }
            labeled.add(new DebtCustomerItem(
                    item.customerId,
                    item.name,
                    item.phone,
                    villageWithDistance,
                    item.totalDebt,
                    item.activeBillDebt,
                    item.nearestDueDate,
                    item.lastBillDate
            ));
        }
        return labeled;
    }

    private double[] getVillageLatLon(String village, Location currentLocation) {
        String cacheKey = village.toLowerCase(Locale.getDefault());
        if (geocodeCache.containsKey(cacheKey)) {
            return geocodeCache.get(cacheKey);
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        Set<String> queries = buildGeocodeQueries(village, currentLocation, geocoder);

        double radiusKm = Math.max(30.0, selectedRangeKm * 2.0);
        double latDelta = kmToLatitudeDelta(radiusKm);
        double lonDelta = kmToLongitudeDelta(radiusKm, currentLocation.getLatitude());

        Address bestAddress = null;
        float bestDistance = Float.MAX_VALUE;

        try {
            for (String query : queries) {
                List<Address> addresses = geocoder.getFromLocationName(
                        query,
                        5,
                        currentLocation.getLatitude() - latDelta,
                        currentLocation.getLongitude() - lonDelta,
                        currentLocation.getLatitude() + latDelta,
                        currentLocation.getLongitude() + lonDelta
                );
                if (addresses == null || addresses.isEmpty()) {
                    continue;
                }
                for (Address address : addresses) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            address.getLatitude(),
                            address.getLongitude(),
                            results
                    );
                    if (results[0] < bestDistance) {
                        bestDistance = results[0];
                        bestAddress = address;
                    }
                }
            }

            if (bestAddress == null) {
                List<Address> global = geocoder.getFromLocationName(village, 1);
                if (global != null && !global.isEmpty()) {
                    bestAddress = global.get(0);
                }
            }

            if (bestAddress != null) {
                double[] result = new double[]{bestAddress.getLatitude(), bestAddress.getLongitude()};
                geocodeCache.put(cacheKey, result);
                return result;
            }
        } catch (IOException ignored) {
        }

        geocodeCache.put(cacheKey, null);
        return null;
    }

    private Set<String> buildGeocodeQueries(String village, Location currentLocation, Geocoder geocoder) {
        Set<String> queries = new LinkedHashSet<>();
        String cleaned = cleanLocationText(village);
        if (!cleaned.isEmpty()) {
            queries.add(cleaned);
            queries.add(cleaned.replace(" ", ""));
        }

        try {
            List<Address> currentAddresses = geocoder.getFromLocation(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    1
            );
            if (currentAddresses != null && !currentAddresses.isEmpty()) {
                Address nearby = currentAddresses.get(0);
                addQueryWithContext(queries, cleaned, nearby.getLocality());
                addQueryWithContext(queries, cleaned, nearby.getSubAdminArea());
                addQueryWithContext(queries, cleaned, nearby.getAdminArea());
                addQueryWithContext(queries, cleaned, nearby.getCountryName());
            }
        } catch (IOException ignored) {
        }

        if (queries.isEmpty()) {
            queries.add(village);
        }
        return queries;
    }

    private void addQueryWithContext(Set<String> queries, String base, String context) {
        String cleanContext = cleanLocationText(context);
        if (base.isEmpty() || cleanContext.isEmpty()) {
            return;
        }
        queries.add(base + ", " + cleanContext);
    }

    private String cleanLocationText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double kmToLatitudeDelta(double km) {
        return km / 111.0;
    }

    private double kmToLongitudeDelta(double km, double latitude) {
        double denominator = 111.320 * Math.cos(Math.toRadians(latitude));
        if (Math.abs(denominator) < 0.0001) {
            return km / 111.320;
        }
        return km / denominator;
    }

    @Nullable
    private Location getBestLastKnownLocation() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) {
            return null;
        }

        Location gps = null;
        Location network = null;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (gps == null) {
            return network;
        }
        if (network == null) {
            return gps;
        }
        return gps.getTime() >= network.getTime() ? gps : network;
    }

    private void openDebtHistory(DebtCustomerItem item) {
        DebtHistoryDialogFragment dialog = DebtHistoryDialogFragment.newInstance(item.customerId, item.name);
        dialog.show(getParentFragmentManager(), "debt_history");
    }
}
