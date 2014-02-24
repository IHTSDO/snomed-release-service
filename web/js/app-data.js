// Configure REST location
DS.RESTAdapter.reopen({
	namespace: 'api/v1'
});

$.ajaxSetup({
	beforeSend: function(xhr) {
		xhr.setRequestHeader('Authorization', 'Basic ' + btoa('test:'));
	}
})

App.DSModel = DS.Model.extend(Ember.Validations.Mixin);

// Define business model
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

// REST interface adapter. Adds JSON envelope.
App.ApplicationSerializer = DS.RESTSerializer.extend({

	// Create json envelope when receiving
	normalizePayload: function(type, payload) {

		App.ResolveHypermediaLinks(payload);
		
		if (type == 'App.BuildConfig'){
			var uniqueID = (Math.random()+ "").substring(2) + new Date().getTime();
			payload['id'] = uniqueID;
			var payloadJSON = JSON.stringify(payload,null,"\t");
			payload['configStr'] = payloadJSON;
		}		

		var o = {};
		o[type.typeKey + 's'] = payload;
		return o;
	},

	// We don't want a json envelope when sending
	serializeIntoHash: function(hash, type, record, options) {
		var root = Ember.String.camelize(type.typeKey);
		var serialized = this.serialize(record, options);
		$.extend(hash, serialized);
		console.log('serializeIntoHash');
		console.log(hash);
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

// Many Array Reload Extension
var get = Ember.get;
DS.PromiseArray.reopen({
	reloadLinks: function() {
		var records = get(this, 'content'),
			store = get(records, 'store'),
			owner = get(records, 'owner'),
			type = get(records, 'type'),
			name = get(records, 'name'),
			resolver = Ember.RSVP.defer();

		var meta = owner.constructor.metaForProperty(name);
		var link = owner._data.links[meta.key];
		store.findHasMany(owner, link, meta, resolver);
	}
});