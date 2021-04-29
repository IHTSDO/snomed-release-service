package org.ihtsdo.buildcloud.service.build.transform;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RandomUUIDGenerator implements UUIDGenerator {

	@Override
	public String uuid() {
		return UUID.randomUUID().toString().toLowerCase();
	}

}
