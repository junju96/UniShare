package com.ushareit.core.scheduler;

import android.content.Context;

import com.ushareit.core.Settings;


// a simple request balancer implementation which use the following balance algorithm: 
// 1. enforce some minimum silence between subsequent requests
// 2. use different silence interval according last request's result (either succ or fail)
// 3. hint: usually the silence after success request is larger than the silence after a failed request (eg. 1/2 is one meaningful practice).
public class SimpleRequestBalancer implements IRequestBalancer {

    // create a balancer for specified requests
    public static IRequestBalancer createForSpecificRequests(Context context, String requestsIdentifier, long silenceIfSucc, long slienceIfFail) {
        return new SimpleRequestBalancer(context, requestsIdentifier, silenceIfSucc, slienceIfFail);
    }

    // create a balancer for commands report requests
    public static IRequestBalancer createForCommandsReportRequests(Context context, String requestsIdentifier, long silenceIfSucc, long slienceIfFail, int threshold) {
        return new SimpleRequestBalancer(context, requestsIdentifier, silenceIfSucc, slienceIfFail, threshold);
    }

    // -------------------------------------------------------------------------------------

    private static final String PREFIX = "RB_";

    private Context mContext;          // wee need this context to store some private states that related to the balancer to persistent settings file.
    private String mRequestsIdentifier;// the unique identifier for this kind of requests that this balancer controls
    private long mSilenceIfSucc;       // the silence duration that if previous request (of this kind) succeed
    private long mSilenceIfFail;       // the silence duration that if previous request (of this kind) failed
    private long mCountThreshold;      // if the existing record count exceeds the threshold, ignore time rule

    protected SimpleRequestBalancer(Context context, String requestsIdentifier, long silenceIfSucc, long silenceIfFail) {
        mContext = context;
        mRequestsIdentifier = requestsIdentifier;
        mSilenceIfSucc = silenceIfSucc;
        mSilenceIfFail = silenceIfFail;
    }

    protected SimpleRequestBalancer(Context context, String requestsIdentifier, long silenceIfSucc, long silenceIfFail, long threshold) {
        mContext = context;
        mRequestsIdentifier = requestsIdentifier;
        mSilenceIfSucc = silenceIfSucc;
        mSilenceIfFail = silenceIfFail;
        mCountThreshold = threshold;
    }

    // called before each records upload request, caller should perform subsequent request only if returned true
    public boolean canRequest(int existingCount) {
        if (existingCount == 0)
            return false;

        if (existingCount >= mCountThreshold)
            return true;

        return this.canRequest();
    }

    // called before each request, caller should perform subsequent request only if returned true
    public boolean canRequest() {
        String keySucc = PREFIX + mRequestsIdentifier + ".SUCC";
        String keyFail = PREFIX + mRequestsIdentifier + ".FAIL";

        // check if last request is succ or fail
        long now = System.currentTimeMillis();
        long elapsedSinceLastSucc = now - new Settings(mContext).getLong(keySucc, 0);
        long elapsedSinceLastFail = now - new Settings(mContext).getLong(keyFail, 0);
        boolean lastIsSucc = (elapsedSinceLastSucc < elapsedSinceLastFail);
        
        // allow or refuse this request according to elapsed interval since last request + last request's result and our guard condition (silences between subsequent requests).
        return lastIsSucc ? (elapsedSinceLastSucc > mSilenceIfSucc) : (elapsedSinceLastFail > mSilenceIfFail);
    }

    // called after performed a request to report final result, either success or failed
    public void reportResult(boolean succ) {
        String key = PREFIX + mRequestsIdentifier + (succ ? ".SUCC" : ".FAIL");
        new Settings(mContext).setLong(key, System.currentTimeMillis());
    }
}
