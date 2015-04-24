package org.provebit.hashcache.merkle;

public enum HashType {
	SHA256, 				// Used for all merkle's outside of bitcoin
	DOUBLE_SHA256			// Used for merkle's inside of bitcoin
}
