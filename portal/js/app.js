window.App = Ember.Application.create();

App.Router.map(function() {
	this.resource('release-centre', { path: '/:releaseCentre_id' }, function() {
		this.resource('extension', { path: '/:extension_id' }, function() {
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

App.FIXTURES = [
	{
		id: 'international_release_centre',
		name: 'International Release Centre',
		extensions: [
			{
				id: 'australian_pathology_extension',
				name: 'Australian Pathology Extension',
				packages: [
					{
						id: 'full_package',
						name: 'Full Package'
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

App.PackageRoute = Ember.Route.extend({
	model: function(params) {
		return this.modelFor('extension').packages.findBy('id', params.package_id)
	}
})