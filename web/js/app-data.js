App.DSModel = DS.Model.extend(Ember.Validations.Mixin);

// Models

App.Centre = App.DSModel.extend({
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
	parent: DS.belongsTo('centre'),
	name: DS.attr(),
	products: DS.hasMany('product', { async: true }),
	//TODO: ONLY FOR DEMO, UNTIL WE GET STARRED BUILDS RETURNING FROM API
	isInternationalEdition: function() {
		return this.get('name') === 'SNOMED CT International Edition';
	}.property('name')
});
App.Product = DS.Model.extend({
	parent: DS.belongsTo('extension'),
	name: DS.attr(),
	builds: DS.hasMany('build', { async: true })
});
App.Build = DS.Model.extend({
	product: DS.belongsTo('product'),
	name: DS.attr(),
	config: DS.belongsTo('buildConfig', { async: true }),
	packages: DS.hasMany('package', { async: true }),
	executions: DS.hasMany('execution', { async: true })
});
App.BuildConfig = DS.Model.extend({
	parent: DS.belongsTo('build'),
	configStr: DS.attr()
});
App.Package = DS.Model.extend({
	parent: DS.belongsTo('build'),
	name: DS.attr(),
	status: DS.attr(),
	inputfiles: DS.hasMany('inputFile', { async: true })
});
App.InputFile = DS.Model.extend({
	parent: DS.belongsTo('package'),
	name: DS.attr()
});
App.Execution = DS.Model.extend({
	parent: DS.belongsTo('build'),
	creationTime: DS.attr(),
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
	}.property('status')
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

// Configure REST location
DS.RESTAdapter.reopen({
	namespace: 'api/v1'
});

// API auto authentication
$.ajaxSetup({
	beforeSend: function(xhr) {
		xhr.setRequestHeader('Authorization', 'Basic ' + btoa('test:'));
	}
})
