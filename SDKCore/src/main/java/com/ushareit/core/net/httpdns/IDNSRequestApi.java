package com.ushareit.core.net.httpdns;

import java.util.Map;

public interface IDNSRequestApi {
    boolean fetchAllDns(Map<String, DNSEntity> cachedEntries);
}
