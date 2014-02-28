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
	products: DS.hasMany('product', { async: true })
});
App.Product = DS.Model.extend({
	parent: DS.belongsTo('extension'),
	name: DS.attr(),
	builds: DS.hasMany('build', { async: true })
});
App.Build = DS.Model.extend({
	parent: DS.belongsTo('product'),
	name: DS.attr(),
	config: DS.belongsTo('buildConfig', { async: true }),
	packages: DS.hasMany('package', { async: true })
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
