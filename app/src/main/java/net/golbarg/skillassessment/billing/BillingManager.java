package net.golbarg.skillassessment.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.Collections;
import java.util.List;

/**
 * One-time "Premium" purchase: removes ads and makes every topic free to unlock.
 *
 * <p>The product {@link #PRODUCT_ID} must exist as an in-app product in the Play Console. Until it
 * does (or on devices without Play), the purchase button reports that Premium is unavailable.
 */
public final class BillingManager {
    private static final String TAG = "BillingManager";
    public static final String PRODUCT_ID = "premium_lifetime";
    /**
     * Whether Premium is offered in the UI. Off while the app is free; existing purchases are still
     * restored in the background so past buyers keep their benefits.
     */
    public static final boolean FOR_SALE = false;

    private static final String FILE = "billing";
    private static final String KEY_PREMIUM = "premium";

    public static final class State {
        public final boolean premium;
        public final boolean available;
        @Nullable public final String price;

        State(boolean premium, boolean available, @Nullable String price) {
            this.premium = premium;
            this.available = available;
            this.price = price;
        }
    }

    public interface Callback {
        void onResult(boolean success);
    }

    private static volatile BillingManager instance;

    private final Context appContext;
    private final BillingClient client;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private ProductDetails product;
    @Nullable private Callback pendingPurchaseCallback;

    public static BillingManager get(Context context) {
        if (instance == null) {
            synchronized (BillingManager.class) {
                if (instance == null) instance = new BillingManager(context.getApplicationContext());
            }
        }
        return instance;
    }

    /** Cached flag, readable synchronously from anywhere (e.g. before showing an ad). */
    public static boolean isPremium(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_PREMIUM, false);
    }

    private BillingManager(Context context) {
        appContext = context;
        client = BillingClient.newBuilder(context)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        state.setValue(new State(isPremium(context), false, null));
    }

    public LiveData<State> getState() {
        return state;
    }

    /** Connects, restores any existing purchase and loads the price. Safe to call repeatedly. */
    public void refresh() {
        if (client.isReady()) {
            queryProduct();
            restore(null);
            return;
        }
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    queryProduct();
                    restore(null);
                } else {
                    Log.d(TAG, "Billing unavailable: " + result.getDebugMessage());
                    publish();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                publish();
            }
        });
    }

    private void queryProduct() {
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()))
                .build();
        client.queryProductDetailsAsync(params, (result, details) -> {
            List<ProductDetails> list = details.getProductDetailsList();
            product = list.isEmpty() ? null : list.get(0);
            publish();
        });
    }

    /** Opens the Play purchase sheet. {@code callback} reports whether Premium became active. */
    public void purchase(Activity activity, Callback callback) {
        if (product == null) {
            callback.onResult(false);
            refresh();
            return;
        }
        pendingPurchaseCallback = callback;
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(
                        BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()))
                .build();
        BillingResult result = client.launchBillingFlow(activity, params);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            pendingPurchaseCallback = null;
            callback.onResult(false);
        }
    }

    /** Looks for an existing purchase (e.g. after reinstalling). */
    public void restore(@Nullable Callback callback) {
        if (!client.isReady()) {
            if (callback != null) callback.onResult(isPremium(appContext));
            refresh();
            return;
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (result, purchases) -> {
                    boolean owned = false;
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase p : purchases) owned |= handle(p);
                        setPremium(owned);
                    }
                    if (callback != null) callback.onResult(owned);
                });
    }

    private void onPurchasesUpdated(@NonNull BillingResult result, @Nullable List<Purchase> purchases) {
        boolean owned = false;
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) owned |= handle(p);
            if (owned) setPremium(true);
        } else if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            restore(pendingPurchaseCallback);
            pendingPurchaseCallback = null;
            return;
        }
        Callback callback = pendingPurchaseCallback;
        pendingPurchaseCallback = null;
        if (callback != null) callback.onResult(owned);
    }

    /** @return true if this purchase grants Premium. Acknowledges it so Play doesn't refund it. */
    private boolean handle(Purchase purchase) {
        if (!purchase.getProducts().contains(PRODUCT_ID)) return false;
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return false;
        if (!purchase.isAcknowledged()) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                    r -> Log.d(TAG, "Acknowledged: " + r.getResponseCode()));
        }
        return true;
    }

    private void setPremium(boolean premium) {
        SharedPreferences prefs = appContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PREMIUM, premium).apply();
        publish();
    }

    private void publish() {
        String price = null;
        if (product != null && product.getOneTimePurchaseOfferDetails() != null) {
            price = product.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }
        state.postValue(new State(isPremium(appContext), product != null, price));
    }
}
