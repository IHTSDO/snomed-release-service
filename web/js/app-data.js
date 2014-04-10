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
	manifest: DS.belongsTo('inputFile', { async: true })
});
App.InputFile = DS.Model.extend({
	parent: DS.belongsTo('package', { inverse: 'inputfiles' }),
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
