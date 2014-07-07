package org.ihtsdo.buildcloud.service.execution.transform;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class PesudoUUIDGenerator implements UUIDGenerator {
    

    private static final String SEED = "5f16e2d7-16d7-41d9-8fdc-8505bff0fc0f";
    private final AtomicInteger count = new AtomicInteger(1);

    @Override
    public String uuid() {
	
	UUID seed = UUID.fromString(SEED);
	UUID next = new UUID(seed.getMostSignificantBits(), 
		seed.getLeastSignificantBits() + count.getAndIncrement());
	return next.toString().toLowerCase();
    }

}
