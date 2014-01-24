function debug (msg) {
	if (window.console) console.log(msg);
}

App = Ember.Application.create({
	ApplicationController: Ember.Controller.extend({
		routeChanged: function(){
			//Initialise popovers for all elements that include the relevant attribute
			//Needs to be repeated each time the DOM changes
			Ember.run.scheduleOnce('afterRender', this, function() {
				$("[data-toggle='popover']").popover();
			});
		}.observes('currentPath'),		
	})
});

// Configure REST location
DS.RESTAdapter.reopen({
	namespace: 'api/v1'
});

// Define business model
App.Centre = DS.Model.extend({
	name: DS.attr(),
	extensions: DS.hasMany('extension', { async: true })
});
App.Extension = DS.Model.extend({
	parent: DS.belongsTo('centre'),
	name: DS.attr(),
	products: DS.hasMany('product', { async: true })
});
App.Product = DS.Model.extend({
	parent: DS.belongsTo('extension'),
	name: DS.attr(),
	didLoad: function() {
		// Add static mock-up data to product.
		var product = this;
		product.packages = $.extend(true, [], App.packages);
		$.each(product.packages, function(index, aPackage) {
			aPackage.parent = product;
		})
	}
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

App.AuthorisedRoute =  App.AbstractRoute.extend({
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
	},
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
	},
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

// REST interface adapter. Adds JSON envelope.
App.ApplicationSerializer = DS.RESTSerializer.extend({
	normalizePayload: function(type, payload) {

App.BuildInputRoute = App.AuthorisedRoute.extend({
	model: function(params) {
		return this.modelFor('package');
	}
})


		App.ResolveHypermediaLinks(payload);

		var o = {};
		o[type.typeKey + 's'] = payload;
		return o;
	}
});

App.ResolveHypermediaLinks = function(object) {
	if($.isArray(object)) {
		$(object).each(function(index, element) {
			App.ResolveHypermediaLinks(element);
		})
	} else {
		var links = {};
		var linkFound = false;
		for (var property in object) {
			if (object.hasOwnProperty(property)) {
				var urlPostfixIndex = property.indexOf('_url');
				if (urlPostfixIndex > 0 && urlPostfixIndex == property.length - 4) {
					links[property.substring(0, urlPostfixIndex)] = object[property];
					linkFound = true;
				}
			}
		}
		if (linkFound) {
			object.links = links;
		}
	}
}


// Static Data
App.packages = [
	{
		id: 'release',
		name: 'Release',
		status: 'status_ok',
		inputFiles: [
			{
				source: 'File',
				operation: 'Fill-Placeholders',
				name: 'readme.txt',
				directory: '/'
			},
			{
				source: 'File',
				operation: '',
				name: 'Icd10MapTechnicalGuideExemplars.xlsx',
				directory: '/Documentation/'
			},
			{
				source: 'File',
				operation: '',
				name: 'SnomedCTReleaseNotes.pdf',
				directory: '/Documentation/'
			},
			{
				source: 'Maven org.ihtsdo:snomedct-rf2:2015-06-01:zip',
				operation: 'Extract',
				name: '',
				directory: '/RF2Release/'
			}
		]
	},
	{
		id: 'rf1compatibilitypackage',
		name: 'RF1CompatibilityPackage',
		status: 'status_ok'
	},
	{
		id: 'rf2torf1conversion',
		name: 'RF2toRF1Conversion',
		status: 'status_warning'
	}
];

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

