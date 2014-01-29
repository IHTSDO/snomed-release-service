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
	didLoad: function() {
		// Add static mock-up data to product.
		var product = this;
		product.packages = $.extend(true, [], App.packages);
		$.each(product.packages, function(index, aPackage) {
			aPackage.parent = product;
		})
	}
});

// REST interface adapter. Adds JSON envelope.
App.ApplicationSerializer = DS.RESTSerializer.extend({
	normalizePayload: function(type, payload) {

		App.ResolveHypermediaLinks(payload);

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

// Static Data
App.packages = [
	{
		id: 'release',
		name: 'Release',
		status: 'status_ok',
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
		name: 'RF1CompatibilityPackage',
		status: 'status_ok'
	},
	{
		id: 'rf2torf1conversion',
		name: 'RF2toRF1Conversion',
		status: 'status_warning'
	}
];