package com.ushareit.core.scheduler;

// a balancer controls the frequency of requests (mostly network requests), to avoid too much pressure on servers.
// usually one instance of balancer for one kind of requests that need to be controlled independently.
public interface IRequestBalancer {
    // called before upload command reports with existing records count
    public boolean canRequest(int existingCount);

    // called before each request, caller should perform subsequent request only if returned true
    public boolean canRequest();

    // called after performed a request to report final result, either success or failed
    public void reportResult(boolean succ);
}
