package com.example.currencyconverter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Offering;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.PurchaseParams;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreTransaction;

import java.net.MalformedURLException;
import java.net.URL;

final class ProDemo {
    private static final String TAG = "CurrencyConverterPro";
    private static final String FROM = "from";
    private static final String TO = "to";

    private final MainActivity activity;
    private final Button buyPro;
    private final TextView status;
    private final Button saveFavourite;
    private final Button useFavourite;

    static void bind(MainActivity activity, Spinner from, Spinner to, ArrayAdapter<String> currencies) {
        new ProDemo(activity, from, to, currencies).start();
    }

    private ProDemo(MainActivity activity, Spinner from, Spinner to, ArrayAdapter<String> currencies) {
        this.activity = activity;
        buyPro = activity.findViewById(R.id.buy_pro);
        status = activity.findViewById(R.id.pro_status);
        saveFavourite = activity.findViewById(R.id.save_favourite);
        useFavourite = activity.findViewById(R.id.use_favourite);

        SharedPreferences pair = activity.getSharedPreferences("favourite_pair", Context.MODE_PRIVATE);
        Runnable updateFavourite = () -> {
            String savedFrom = pair.getString(FROM, "");
            String savedTo = pair.getString(TO, "");
            int fromPosition = currencies.getPosition(savedFrom);
            int toPosition = currencies.getPosition(savedTo);
            boolean hasPair = fromPosition >= 0 && toPosition >= 0;
            useFavourite.setEnabled(hasPair);
            useFavourite.setText(hasPair ? activity.getString(R.string.use_saved_pair, savedFrom, savedTo)
                    : activity.getString(R.string.use_favourite));
            useFavourite.setOnClickListener(view -> {
                from.setSelection(fromPosition);
                to.setSelection(toPosition);
            });
        };
        saveFavourite.setOnClickListener(view -> {
            pair.edit().putString(FROM, from.getSelectedItem().toString())
                    .putString(TO, to.getSelectedItem().toString()).apply();
            updateFavourite.run();
        });
        updateFavourite.run();
        activity.findViewById(R.id.pro_controls).setVisibility(View.VISIBLE);
    }

    private void start() {
        String key = BuildConfig.REVENUECAT_TEST_STORE_KEY;
        buyPro.setEnabled(false);
        if (!key.startsWith("test_") || key.length() == "test_".length()) {
            status.setText(R.string.pro_unavailable);
            return;
        }

        if (!Purchases.isConfigured()) {
            if (BuildConfig.REVENUECAT_TEST_STORE_USE_BACKUP) {
                try {
                    Purchases.setProxyURL(new URL("https://api.rc-backup.com/"));
                } catch (MalformedURLException error) {
                    Log.w(TAG, "Invalid RevenueCat backup URL", error);
                }
            }
            Purchases.configure(new PurchasesConfiguration.Builder(activity.getApplicationContext(), key).build());
        }
        Purchases purchases = Purchases.getSharedInstance();
        buyPro.setOnClickListener(view -> buy(purchases));
        status.setText(R.string.pro_checking);
        purchases.getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(CustomerInfo customerInfo) {
                showAccess(customerInfo, R.string.pro_locked);
            }

            @Override
            public void onError(PurchasesError error) {
                Log.w(TAG, "CustomerInfo: " + error.getCode());
                showLocked(R.string.pro_check_error);
            }
        });
    }

    private void buy(Purchases purchases) {
        buyPro.setEnabled(false);
        status.setText(R.string.pro_loading_purchase);
        purchases.getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(Offerings offerings) {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                Offering current = offerings.getCurrent();
                Package lifetime = current == null ? null : current.getLifetime();
                if (lifetime == null) {
                    showLocked(R.string.pro_package_missing);
                    return;
                }
                purchase(purchases, lifetime);
            }

            @Override
            public void onError(PurchasesError error) {
                Log.w(TAG, "Offerings: " + error.getCode());
                showLocked(R.string.pro_offering_error);
            }
        });
    }

    private void purchase(Purchases purchases, Package lifetime) {
        purchases.purchase(new PurchaseParams.Builder(activity, lifetime).build(), new PurchaseCallback() {
            @Override
            public void onCompleted(StoreTransaction transaction, CustomerInfo customerInfo) {
                showAccess(customerInfo, R.string.pro_entitlement_missing);
            }

            @Override
            public void onError(PurchasesError error, boolean userCancelled) {
                Log.d(TAG, "Test purchase: " + error.getCode());
                showLocked(userCancelled ? R.string.pro_cancelled : R.string.pro_purchase_failed);
            }
        });
    }

    private void showAccess(CustomerInfo customerInfo, int lockedMessage) {
        if (!customerInfo.getEntitlements().getActive().containsKey("pro")) {
            showLocked(lockedMessage);
        } else if (!activity.isFinishing() && !activity.isDestroyed()) {
            buyPro.setVisibility(View.GONE);
            status.setText(R.string.pro_unlocked);
            saveFavourite.setVisibility(View.VISIBLE);
            useFavourite.setVisibility(View.VISIBLE);
        }
    }

    private void showLocked(int message) {
        if (!activity.isFinishing() && !activity.isDestroyed()) {
            status.setText(message);
            buyPro.setEnabled(true);
        }
    }
}
