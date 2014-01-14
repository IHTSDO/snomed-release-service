window.App = Ember.Application.create();

App.Router.map(function() {
	this.resource('configuration');
	this.resource('build-input');
	this.resource('pre-conditions');
	this.resource('post-conditions');	
	this.resource('build-trigger');
	this.resource('build-results');
});
