/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.gbaldera.tipaypal;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;
import org.appcelerator.titanium.util.TiConvert;

import org.json.JSONException;

import java.math.BigDecimal;
import java.util.HashMap;

@Kroll.module(name="Tipaypal", id="com.gbaldera.tipaypal")
public class TipaypalModule extends KrollModule implements TiActivityResultHandler
{

	// Standard Debugging variables
	private static final String TAG = "TipaypalModule";
	private String client_id;
	private String receiver_email;
	private String environment;
	private Boolean skip_credit_card;

	// constants
	@Kroll.constant public static final String PARAM_ENVIRONMENT = "environment";
	@Kroll.constant public static final String PARAM_CLIENT_ID = "client_id";
	@Kroll.constant public static final String PARAM_PAYER_ID = "payer_id";
	@Kroll.constant public static final String PARAM_RECEIVER_EMAIL = "receiver_email";
	@Kroll.constant public static final String PARAM_CURRENCY = "currency";
	@Kroll.constant public static final String PARAM_AMOUNT = "amount";
	@Kroll.constant public static final String PARAM_DESCRIPTION = "description";
	@Kroll.constant public static final String PARAM_SKIP_CREDIT_CARD = "skip_credit_card";

	@Kroll.constant public static final String EVENT_COMPLETED = "completed";
	@Kroll.constant public static final String EVENT_ERROR = "error";
	@Kroll.constant public static final String EVENT_CANCELLED = "cancelled";
	@Kroll.constant public static final String EVENT_PAYMENT_INVALID = "paymentinvalid";

	@Kroll.constant public static final String ENVIRONMENT_NO_NETWORK = PaymentActivity.ENVIRONMENT_NO_NETWORK;
	@Kroll.constant public static final String ENVIRONMENT_SANDBOX = PaymentActivity.ENVIRONMENT_SANDBOX;
	@Kroll.constant public static final String ENVIRONMENT_PRODUCTION = PaymentActivity.ENVIRONMENT_PRODUCTION;

	public TipaypalModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(TAG, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}

    @Override
    public void onDestroy(Activity activity)
    {
        // This method is called when the root context is being destroyed
        activity.stopService(new Intent(activity, PayPalService.class));

        Log.d(TAG, "[MODULE LIFECYCLE EVENT] destroy");
        super.onDestroy(activity);
    }

	// Methods
	@Kroll.method
    public void initialize(HashMap params)
    {
        KrollDict dict = new KrollDict(params);

        client_id = TiConvert.toString(dict, PARAM_CLIENT_ID);
        receiver_email = TiConvert.toString(dict, PARAM_RECEIVER_EMAIL);
        environment = TiConvert.toString(dict, PARAM_ENVIRONMENT);
        skip_credit_card = TiConvert.toBoolean(dict, PARAM_SKIP_CREDIT_CARD, false);

        if(environment == null)
        {
            environment = ENVIRONMENT_NO_NETWORK;
        }

        if(client_id == null || receiver_email == null)
        {
            Log.w(TAG, "The CLIENT ID and RECEIVER EMAIL params are required");
            return;
        }

        Log.d(TAG, PARAM_ENVIRONMENT + ": " + environment);

        Activity currentActivity = TiApplication.getAppCurrentActivity();
        Intent intent = new Intent(currentActivity, PayPalService.class);

        if(isPayPalServiceRunning()) // if the service is already running stop it first
        {
            currentActivity.stopService(intent);
        }

        // set to PaymentActivity.ENVIRONMENT_PRODUCTION to move real money.
        // set to PaymentActivity.ENVIRONMENT_SANDBOX to use your test credentials from https://developer.paypal.com
        // set to PaymentActivity.ENVIRONMENT_NO_NETWORK to kick the tires without communicating to PayPal's servers.
        intent.putExtra(PaymentActivity.EXTRA_PAYPAL_ENVIRONMENT, environment);

        intent.putExtra(PaymentActivity.EXTRA_RECEIVER_EMAIL, receiver_email);
        intent.putExtra(PaymentActivity.EXTRA_CLIENT_ID, client_id);

        currentActivity.startService(intent);
    }

    @Kroll.method
    public void doPayment(HashMap params)
    {
        if(!isPayPalServiceRunning())
        {
            Log.w(TAG, "The PayPalService is not Running. Be sure to call the initialize method first");
            return;
        }

        // TODO: verify the required params

        KrollDict dict = new KrollDict(params);

        Activity activity = TiApplication.getAppCurrentActivity();

        PayPalPayment payment = new PayPalPayment(
                new BigDecimal(TiConvert.toString(dict, PARAM_AMOUNT)),
                TiConvert.toString(dict.get(PARAM_CURRENCY), "USD"),
                TiConvert.toString(dict, PARAM_DESCRIPTION)
        );

        Intent intent = new Intent(activity, PaymentActivity.class);

        // comment this line out for live or set to PaymentActivity.ENVIRONMENT_SANDBOX for sandbox
        intent.putExtra(PaymentActivity.EXTRA_PAYPAL_ENVIRONMENT, environment);

        // it's important to repeat the clientId here so that the SDK has it if Android restarts your
        // app midway through the payment UI flow.
        intent.putExtra(PaymentActivity.EXTRA_CLIENT_ID, client_id);

        // Provide a payerId that uniquely identifies a user within the scope of your system,
        // such as an email address or user ID.
        intent.putExtra(PaymentActivity.EXTRA_PAYER_ID, TiConvert.toString(dict, PARAM_PAYER_ID));

        intent.putExtra(PaymentActivity.EXTRA_RECEIVER_EMAIL, receiver_email);

        // disable credit card acceptance ?
        intent.putExtra(PaymentActivity.EXTRA_SKIP_CREDIT_CARD, skip_credit_card);

        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        TiActivitySupport activitySupport = (TiActivitySupport) activity;
        final int resultCode = activitySupport.getUniqueResultCode();
        activitySupport.launchActivityForResult(intent, resultCode, this);
    }

    @Override
    public void onResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK)
        {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);

            if (confirm != null) {
                try {

                    // send 'confirm' to your server for verification.
                    // see https://developer.paypal.com/webapps/developer/docs/integration/mobile/verify-mobile-payment/
                    // for more details.

                    Log.i(TAG, confirm.toJSONObject().toString(4));
                    processResult(resultCode, new KrollDict(confirm.toJSONObject()));

                } catch (JSONException e) {
                    processFailed(resultCode, e);
                }
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            processCanceled(resultCode);
        }
        else if (resultCode == PaymentActivity.RESULT_PAYMENT_INVALID) {
            processInvalid(resultCode);
        }
    }

    @Override
    public void onError(Activity activity, int requestCode, Exception e) {
        HashMap<String, Object> errorDict = new HashMap<String, Object>();
        errorDict.put("message", "Payment Failed");
        errorDict.put("code", requestCode);
        fireEvent(EVENT_ERROR, errorDict);
    }

    private Boolean isPayPalServiceRunning()
    {
        ActivityManager manager = (ActivityManager) TiApplication.getAppCurrentActivity().getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if(PayPalService.class.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }

        return false;
    }

    private void processFailed(int resultCode, Exception e) {

        String message = "An extremely unlikely failure occurred: " + e.getMessage();
        Log.e(TAG, message, e);

        if (hasListeners(EVENT_ERROR)) {
            HashMap<String, Object> errorDict = new HashMap<String, Object>();
            errorDict.put("message", message);
            errorDict.put("code", resultCode);
            fireEvent(EVENT_ERROR, errorDict);
        }
    }

    private void processInvalid(int resultCode) {

        String message = "An invalid payment was submitted. Please see the docs.";
        Log.i(TAG, message);

        if (hasListeners(EVENT_PAYMENT_INVALID)) {
            HashMap<String, Object> errorDict = new HashMap<String, Object>();
            errorDict.put("message", message);
            errorDict.put("code", resultCode);
            fireEvent(EVENT_PAYMENT_INVALID, errorDict);
        }
    }

    private void processCanceled(int resultCode) {

        String message = "The user canceled.";
        Log.i(TAG, message);

        if (hasListeners(EVENT_CANCELLED)) {
            HashMap<String, Object> cancelDict = new HashMap<String, Object>();
            cancelDict.put("message", message);
            cancelDict.put("code", resultCode);
            fireEvent(EVENT_CANCELLED, cancelDict);
        }
    }

    private void processResult(int resultCode, KrollDict confirmation) {

        if (hasListeners(EVENT_COMPLETED)) {
            HashMap<String, Object> dict = new HashMap<String, Object>();
            dict.put("code", resultCode);
            dict.put("confirmation", confirmation);
            fireEvent(EVENT_COMPLETED, dict);
        }
    }
}

