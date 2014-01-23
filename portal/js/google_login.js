function debug (msg) {
	if (window.console) console.log(msg);
}

function signinCallback(authResult) {
	if (authResult['status']['signed_in']) {
		// Hide the sign-in button now that the user is authorized
		debug ("SignIn OK! Hiding Button for " + authResult['access_token']);
		$(".toggle").toggle();
		
		//Now store the token in the Ember App
		window.App.authorisationToken = authResult['access_token'];
		
	} else {
		debug('Sign-in state: ' + authResult['error']);
		window.App.authorisationToken = "Athentication Failed in Client";
	}
}


