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

// Index
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
		return App.FIXTURES.findBy('id', params.releaseCentre_id);
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
		return this.modelFor('release-centre').extensions.findBy('id', params.extension_id);
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

App.BuildInputRoute = Ember.Route.extend({
	model: function(params) {
		return this.modelFor('package');
	}
})

// Static Data
App.FIXTURES = [
	{
		id: 'international',
		name: 'International Release Centre',
		extensions: [
			{
				id: 'international_edition',
				name: 'SNOMED CT International Edition',
				products: [
					{
						id: 'international_edition',
						name: 'SNOMED CT International Edition',
						packages: [
							{
								id: 'release',
								name: 'Release',
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
								name: 'RF1CompatibilityPackage'
							},
							{
								id: 'rf2torf1conversion',
								name: 'RF2toRF1Conversion'
							}
						]
					},
					{
						id: 'spanish',
						name: 'SNOMED CT Spanish Edition',
						packages: [
							{
								id: 'release',
								name: 'Release'
							},
							{
								id: 'rf1compatibilitypackage',
								name: 'RF1CompatibilityPackage'
							},
							{
								id: 'rf2torf1conversion',
								name: 'RF2toRF1Conversion'
							}
						]
					}
				]
			}
		]
	}
];
// Add parent references to assist breadcrumb
for (var rcIndex = 0; rcIndex < App.FIXTURES.length; rcIndex++) {
	var rc = App.FIXTURES[rcIndex];
	for (var eIndex = 0; eIndex < rc.extensions.length; eIndex++) {
		var e = rc.extensions[eIndex];
		e.parent = rc;
		for (var productIndex = 0; productIndex < e.products.length; productIndex++) {
			var product = e.products[productIndex];
			product.parent = e;
			for (var packageIndex = 0; packageIndex < product.packages.length; packageIndex++) {
				var pack = product.packages[packageIndex];
				pack.parent = product;
			}
		}
	}
}