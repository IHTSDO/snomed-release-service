
App = Ember.Application.create({
	ApplicationController: Ember.Controller.extend({
		routeChanged: function(){
			//Initialise popovers for all elements that include the relevant attribute
			//Needs to be repeated each time the DOM changes
			Ember.run.scheduleOnce('afterRender', this, function() {
				$("[data-toggle='popover']").popover();
			});
		}.observes('currentPath')
	})
});


App.Router.map(function() {
	this.resource('pre-login');
	this.resource('release-centre', { path: '/:releaseCentre_id' }, function() {
		this.resource('extension', { path: '/:extension_id' }, function() {
			this.resource('product', { path: '/:product_id' }, function() {
				this.resource('package', { path: '/:package_id'}, function() {
					this.resource('configuration');
					this.resource('build-input');
					this.resource('pre-conditions');
					this.resource('post-conditions');
					this.resource('build-trigger');
					this.resource('build-results');
					this.resource('build-history');					
				});
			});
		});
	});
});


App.AbstractRoute = Ember.Route.extend({
	init: function() {
		debug ("Setting last known route in " + this);
		App.set('lastKnownRoute', this);
	}
});

App.AuthorisedRoute = App.AbstractRoute.extend({
	beforeModel: function() {
		//Redirect user to login page if no authorisation token is stored.
		if (sessionStorage.authorisationToken === undefined){
			this.transitionTo('pre-login');
		}
	}
});

//Pre-Login
App.PreLoginRoute = App.AbstractRoute.extend();

// Index
App.IndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return {
			releaseCentres: this.store.find('centre')
		}
	}
})

// ReleaseCentre
App.ReleaseCentreRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.store.find('centre', params.releaseCentre_id);
	}
})
App.ReleaseCentreIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('release-centre');
	}
})

// Extension
App.ExtensionRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var centre = this.modelFor('release-centre');
		return centre.get('extensions').then(function(extensions) {
			var extension = extensions.findBy('id', params.extension_id);
			extension.set('parent', centre);
			return  extension;
		});
	}
})
App.ExtensionIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('extension');
	}
})

// Product
App.ProductRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		var extension = this.modelFor('extension');
		return extension.get('products').then(function(products) {
			var product = products.findBy('id', params.product_id);
			product.set('parent', extension);
			return product;
		});
	}
})
App.ProductIndexRoute = App.AuthorisedRoute.extend({
	model: function() {
		return this.modelFor('product');
	}
})

// Package
App.PackageRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('product').packages.findBy('id', params.package_id)
	}
})

App.BuildInputRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	}
})


function signinCallback(authResult) {
	if (authResult['status']['signed_in']) {
		//Store the token in session storage.  Note we can't store against Ember
		//App or it will be lost on page reload.
		sessionStorage.authorisationToken = authResult['access_token'];
		
		//And return the user to whatever page there were on when they got booted here
		var currentRoute = App.get('lastKnownRoute');
		currentRoute.transitionTo('index');
	} else {
		debug('Sign-in state: ' + authResult['error']);
		sessionStorage.authorisationToken = "Athentication Failed in Client";
	}
}

function debug(msg) {
	if (window.console) console.log(msg);
}
