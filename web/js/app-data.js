// Configure REST location
DS.RESTAdapter.reopen({
	namespace: 'api/v1'
});

// Define business model
App.Centre = DS.Model.extend({
	name: DS.attr(),
	extensions: DS.hasMany('extension', { async: true })
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
	inputFiles: DS.hasMany('inputFile',{async: true})
});
App.InputFile = DS.Model.extend({
	parent: DS.belongsTo('package'),
	name: DS.attr()
});

// REST interface adapter. Adds JSON envelope.
App.ApplicationSerializer = DS.RESTSerializer.extend({
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