App.DSModel = DS.Model.extend(Ember.Validations.Mixin);

// Models

App.Center = App.DSModel.extend({
	name: DS.attr(),
	shortName: DS.attr(),
	extensions: DS.hasMany('extension', { async: true }),
	removed: DS.attr('boolean'),
	validations: {
		name: {
			presence: true,
			length: { minimum: 3 }
		},
		shortName: {
			presence: true,
			length: { minimum: 3 }
		}
	}
});
App.Extension = DS.Model.extend({
	parent: DS.belongsTo('center'),
	name: DS.attr(),
	products: DS.hasMany('product', { async: true }),
	isInternationalEdition: function() {
		return this.get('id') === 'snomed_ct_international_edition';
	}.property('name')
});
App.Product = DS.Model.extend({
	parent: DS.belongsTo('extension'),
	name: DS.attr(),
	builds: DS.hasMany('build', { async: true })
});
var activeBuilds=['1_20140731_international_release_build'];
App.Build = DS.Model.extend({
	product: DS.belongsTo('product'),
	name: DS.attr(),
	config: DS.belongsTo('buildConfig', { async: true }),
	packages: DS.hasMany('package', { async: true }),
	executions: DS.hasMany('execution', { async: true }),
	isDemoData: function () {
		for (var i=0; i<activeBuilds.length; i++){
			if (activeBuilds[i] == this.get('id'))
				return false;
		}
		return true;
	}.property('name')
});
App.BuildConfig = DS.Model.extend({
	parent: DS.belongsTo('build'),
	configStr: DS.attr()
});
App.Package = DS.Model.extend({
	parent: DS.belongsTo('build'),
	name: DS.attr(),
	status: DS.attr(),
	inputfiles: DS.hasMany('inputFile', { async: true }),
	manifest: DS.belongsTo('manifest', { async: true })
});
App.InputFile = DS.Model.extend({
	parent: DS.belongsTo('package', { inverse: 'inputfiles' }),
	name: DS.attr()
});
App.Manifest = DS.Model.extend({
	parent: DS.belongsTo('package', { inverse: 'manifest' }),
	name: DS.attr()
});
var demoExecutions=['2014-03-31T09:30:23'];
App.Execution = DS.Model.extend({
	parent: DS.belongsTo('build'),
	creationTimeString: DS.attr(),
	status: DS.attr(),
	buildScripts_url: DS.attr(),
	configuration: DS.belongsTo('executionConfiguration', { async: true }),
	statusTitle: function() {
		var status = this.get('status');
		switch (status) {
			case App.ExecutionStatus.BEFORE_TRIGGER:
				return 'Before Trigger'
		}
	}.property('status'),
	isNotTriggered: function() {
		return this.get('status') == App.ExecutionStatus.BEFORE_TRIGGER;
	}.property('status'),
	isTriggered: function() {
		return this.get('status') != App.ExecutionStatus.BEFORE_TRIGGER;
	}.property('status'),
	isBuilt: function() {
		return this.get('status') == App.ExecutionStatus.BUILT;
	}.property('status'),
	creationTime: function() {
		return moment(this.get('creationTimeString')).format('DD MMM, YYYY hh:mm:ss (UTC)');
	}.property('creationTimeString'),
	isDemoData: function () {
		for (var i=0; i<demoExecutions.length; i++){
			if (demoExecutions[i] == this.get('id'))
				return true;
		}
		return false;
	}.property('creationTime')
});
App.ExecutionStatus = {
	BEFORE_TRIGGER: 'BEFORE_TRIGGER',
	QUEUED: 'QUEUED',
	BUILDING: 'BUILDING',
	BUILT: 'BUILT'
}
App.ExecutionConfiguration = DS.Model.extend({
	dummy: DS.attr(),
	json: function() {
		return JSON.stringify(this._data, null, 2);
	}.property('dummy')
});


// Configuration

App.namespace = 'api/v1';
// Configure REST location
DS.RESTAdapter.reopen({
	namespace: App.namespace,
	pathForType: function(type) {
		if (type == App.User) {
			return "user"
		} else {
			return this._super(type);
		}
	}
});

// Load the current user and validate any sessionStorage authenticationToken
App.loadCurrentUser = function() {
	delete App.currentUser;
	delete App.authenticationToken;
	function doLoadCurrentUser(attempt) {
		var user = null;
		if (attempt <= 2) {
			$.ajax({
				async: false,
				url: App.namespace + '/user',
				beforeSend: function(xhr, settings) {
					if (sessionStorage.authenticationToken) {
						xhr.setRequestHeader('Authorization', 'Basic ' + btoa(sessionStorage.authenticationToken + ':'));
					}
				},
				success: function(data) {
					user = data;
					if (user.authenticated == true) {
						// sessionStorage authenticationToken is valid. Push into App for use in all future calls.
						App.authenticationToken = sessionStorage.authenticationToken;
					} else {
						// sessionStorage authenticationToken is invalid, let's clear it out.
						delete sessionStorage.authenticationToken;
					}
				},
				error: function() {
					delete sessionStorage.authenticationToken;
					doLoadCurrentUser(++attempt);
				}
			})
		}
		return user;
	}
	return doLoadCurrentUser(1);
}
App.login = function(username, password) {
	App.clearLogin();
	var error = null;
	var user = null;
	$.ajax({
		async: false,
		url: App.namespace + '/login',
		type: 'POST',
		data: {username: username, password: password},
		success: function(data) {
			sessionStorage.authenticationToken = data.authenticationToken;
			user = App.loadCurrentUser();
		},
		error: function() {
			error = Error();
		}
	})
	if (!error) {
		return user;
	} else {
		throw Error();
	}
}
App.logout = function() {
	App.clearLogin();
	App.clearStore();
	return App.loadCurrentUser();
}
App.clearLogin = function() {
	delete App.authenticationToken;
	delete sessionStorage.authenticationToken;
}
App.clearStore = function() {
	App.store.unloadAll(App.Center);
}

// API auto authentication
$.ajaxSetup({
	beforeSend: function(xhr, settings) {
		if (App.authenticationToken) {
			xhr.setRequestHeader('Authorization', 'Basic ' + btoa(App.authenticationToken + ':'));
		}
	}
})
