window.App = Ember.Application.create();

App.Router.map(function() {
	this.resource('configuration');
	this.resource('build-input');
	this.resource('build-trigger');
	this.resource('build-results');
});
