package com.termmed.genid.ws;

import org.apache.ws.security.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import java.io.IOException;

public class PWCBHandler implements CallbackHandler {

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (int i = 0; i < callbacks.length; i++) {
			
			WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];

			String id = pwcb.getIdentifier();
			if ("client".equals(id)) {
				pwcb.setPassword("apache");
			} else if ("service".equals(id)) {
				pwcb.setPassword("apache");
			}
		}
	}

}
