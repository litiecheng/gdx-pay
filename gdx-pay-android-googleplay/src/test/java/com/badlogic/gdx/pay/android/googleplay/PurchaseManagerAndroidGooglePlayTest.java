package com.badlogic.gdx.pay.android.googleplay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.android.vending.billing.IInAppBillingService;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.utils.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.badlogic.gdx.pay.android.googleplay.PurchaseManagerConfigObjectMother.managerConfigGooglePlayOneOfferBuyFullEditionProduct;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseManagerAndroidGooglePlayTest {

    private static final int REQUEST_CODE = 1032;

    @Mock
    Activity activity;

    @Mock
    Logger logger;

    @Mock
    PurchaseObserver purchaseObserver;

    @Captor
    ArgumentCaptor<ServiceConnection> serviceConnectionArgumentCaptor;

    @Mock
    IInAppBillingService inAppBillingService;


    private PurchaseManagerAndroidGooglePlay purchaseManager;

    @Before
    public void setUp() throws Exception {
        purchaseManager = new PurchaseManagerAndroidGooglePlay(activity, REQUEST_CODE) {
            @Override
            protected IInAppBillingService lookupByStubAsInterface(IBinder binder) {
                return inAppBillingService;
            }
        };
        purchaseManager.logger = logger;

        when(activity.getPackageName()).thenReturn("com.gdx.pay.dummy.activity");
    }

    @Test
    public void shouldNotBeInstalledAfterInstantiation() throws Exception {
        assertFalse(purchaseManager.installed());
    }

    @Test
    public void installShouldStartActivityIntent() throws Exception {
        installWithSimpleProduct();

        verify(activity).bindService(isA(Intent.class), isA(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE));
    }

    @Test
    public void shouldCallObserverInstallErrorOnActivityBindFailure() throws Exception {
        when(activity.bindService(isA(Intent.class), isA(ServiceConnection.class),
                eq(Context.BIND_AUTO_CREATE)))
                .thenThrow(new SecurityException("Not allowed to bind to this service"));

        installWithSimpleProduct();

        verify(purchaseObserver).handleInstallError(isA(GdxPayInstallFailureException.class));
    }

    @Test
    public void shouldRequestSkusWhenConnectSucceeds() throws Exception {

        installWithSimpleProduct();

        verify(activity).bindService(isA(Intent.class), serviceConnectionArgumentCaptor.capture(), eq(Context.BIND_AUTO_CREATE));

        ServiceConnection connection = serviceConnectionArgumentCaptor.getValue();

        // "inapp" of "subs"
        when(inAppBillingService.getSkuDetails(
                        eq(PurchaseManagerAndroidGooglePlay.BILLING_API_VERSION),
                        isA(String.class),
                        eq(PurchaseManagerAndroidGooglePlay.PURCHASE_TYPE_IN_APP),
                        isA(Bundle.class))
        ).thenReturn(new Bundle(1));

        connection.onServiceConnected(null, null);

        verify(inAppBillingService).getSkuDetails(
                eq(PurchaseManagerAndroidGooglePlay.BILLING_API_VERSION),
                isA(String.class),
                eq("inapp"),
                isA(Bundle.class));

    }

    private void installWithSimpleProduct() {
        purchaseManager.install(purchaseObserver, managerConfigGooglePlayOneOfferBuyFullEditionProduct(), false);
    }

}