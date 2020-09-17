package com.ushareit.core.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * manage all services lifecycle in this app (process).
 * that means: if all services finished their job, then we can safely shutdown the whole process.
 */
public class ServiceManager {
	private static final String TAG = "ServiceManager";
	private static Map<String, String> mServiceReferences = new ConcurrentHashMap<>();

	public static String add(String serviceName) {
		String uuid = UUID.randomUUID().toString();
		mServiceReferences.put(uuid, serviceName);
		return uuid;
	}

	public static void remove(String uuid) {
		mServiceReferences.remove(uuid);
	}

	public static boolean exists(String serviceName) {
		return mServiceReferences.containsValue(serviceName);
	}
}
