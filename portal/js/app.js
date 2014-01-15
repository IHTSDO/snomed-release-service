window.App = Ember.Application.create();

App.Router.map(function() {
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
				});
			});
		});
	});
});

App.FIXTURES = [
	{
		id: 'international_release_centre',
		name: 'International Release Centre',
		extensions: [
			{
				id: 'snomed_ct_international_edition',
				name: 'SNOMED CT International Edition',
				products: [
					{
						id: 'snomed_ct_international_edition',
						name: 'SNOMED CT International Edition',
						packages: [
							{
								id: 'snomedct_release_int',
								name: 'SnomedCT Release INT'
							},
							{
								id: 'snomedct_rf1compatibilitypackage_int',
								name: 'SnomedCT RF1CompatibilityPackage INT'
							},
							{
								id: 'snomedct_rf2torf1conversion_int',
								name: 'SnomedCT RF2toRF1Conversion INT'
							}
						]
					},
					{
						id: 'snomed_ct_spanish_edition',
						name: 'SNOMED CT Spanish Edition',
						packages: [
							{
								id: 'snomedct_release_spa',
								name: 'SnomedCT Release SPA'
							},
							{
								id: 'snomedct_rf1compatibilitypackage_spa',
								name: 'SnomedCT RF1CompatibilityPackage SPA'
							},
							{
								id: 'snomedct_rf2torf1conversion_spa',
								name: 'SnomedCT RF2toRF1Conversion SPA'
							}
						]
					}
				]
			}
		]
	}
];

App.IndexRoute = Ember.Route.extend({
	model: function() {
		return {
			releaseCentres: App.FIXTURES
		}
	}
})

// ReleaseCentre
App.ReleaseCentreRoute = Ember.Route.extend({
	model: function(params) {
		var releaseCentre = App.FIXTURES.findBy('id', params.releaseCentre_id);
		return  releaseCentre;
	}
})
App.ReleaseCentreIndexRoute = Ember.Route.extend({
	model: function() {
		return this.modelFor('release-centre');
	}
})

// Extension
App.ExtensionRoute = Ember.Route.extend({
	model: function(params) {
		return this.modelFor('release-centre').extensions.findBy('id', params.extension_id)
	}
})
App.ExtensionIndexRoute = Ember.Route.extend({
	model: function() {
		return this.modelFor('extension');
	}
})

// Product
App.ProductRoute = Ember.Route.extend({
	model: function(params) {
		return this.modelFor('extension').products.findBy('id', params.product_id)
	}
})
App.ProductIndexRoute = Ember.Route.extend({
	model: function() {
		return this.modelFor('product');
	}
})

// Package
App.PackageRoute = Ember.Route.extend({
	model: function(params) {
		return this.modelFor('product').packages.findBy('id', params.package_id)
	}
})