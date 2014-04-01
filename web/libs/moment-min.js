(function(A){function F(){return{empty:!1,unusedTokens:[],unusedInput:[],overflow:-2,charsLeftOver:0,nullInput:!1,invalidMonth:null,invalidFormat:!1,userInvalidated:!1,iso:!1}}function Z(a,b){return function(c){return m(a.call(this,c),b)}}function ta(a,b){return function(c){return this.lang().ordinal(a.call(this,c),b)}}function $(){}function G(a){aa(a);p(this,a)}function H(a){a=ba(a);var b=a.year||0,c=a.month||0,d=a.week||0,f=a.day||0;this._milliseconds=+(a.millisecond||0)+1E3*(a.second||0)+6E4*(a.minute||
0)+36E5*(a.hour||0);this._days=+f+7*d;this._months=+c+12*b;this._data={};this._bubble()}function p(a,b){for(var c in b)b.hasOwnProperty(c)&&(a[c]=b[c]);b.hasOwnProperty("toString")&&(a.toString=b.toString);b.hasOwnProperty("valueOf")&&(a.valueOf=b.valueOf);return a}function v(a){return 0>a?Math.ceil(a):Math.floor(a)}function m(a,b,c){for(var d=""+Math.abs(a);d.length<b;)d="0"+d;return(0<=a?c?"+":"":"-")+d}function I(a,b,c,d){var f=b._milliseconds,g=b._days;b=b._months;var h,k;f&&a._d.setTime(+a._d+
f*c);if(g||b)h=a.minute(),k=a.hour();g&&a.date(a.date()+g*c);b&&a.month(a.month()+b*c);f&&!d&&e.updateOffset(a);if(g||b)a.minute(h),a.hour(k)}function J(a){return"[object Array]"===Object.prototype.toString.call(a)}function ca(a,b,c){var d=Math.min(a.length,b.length),f=Math.abs(a.length-b.length),g=0,e;for(e=0;e<d;e++)(c&&a[e]!==b[e]||!c&&k(a[e])!==k(b[e]))&&g++;return g+f}function n(a){if(a){var b=a.toLowerCase().replace(/(.)s$/,"$1");a=ua[a]||va[b]||b}return a}function ba(a){var b={},c,d;for(d in a)a.hasOwnProperty(d)&&
(c=n(d))&&(b[c]=a[d]);return b}function wa(a){var b,c;if(0===a.indexOf("week"))b=7,c="day";else if(0===a.indexOf("month"))b=12,c="month";else return;e[a]=function(d,f){var g,h,k=e.fn._lang[a],l=[];"number"===typeof d&&(f=d,d=A);h=function(a){a=e().utc().set(c,a);return k.call(e.fn._lang,a,d||"")};if(null!=f)return h(f);for(g=0;g<b;g++)l.push(h(g));return l}}function k(a){a=+a;var b=0;0!==a&&isFinite(a)&&(b=0<=a?Math.floor(a):Math.ceil(a));return b}function K(a){return 0===a%4&&0!==a%100||0===a%400}
function aa(a){var b;a._a&&-2===a._pf.overflow&&(b=0>a._a[w]||11<a._a[w]?w:1>a._a[t]||a._a[t]>(new Date(Date.UTC(a._a[r],a._a[w]+1,0))).getUTCDate()?t:0>a._a[q]||23<a._a[q]?q:0>a._a[x]||59<a._a[x]?x:0>a._a[B]||59<a._a[B]?B:0>a._a[C]||999<a._a[C]?C:-1,a._pf._overflowDayOfYear&&(b<r||b>t)&&(b=t),a._pf.overflow=b)}function da(a){null==a._isValid&&(a._isValid=!isNaN(a._d.getTime())&&0>a._pf.overflow&&!a._pf.empty&&!a._pf.invalidMonth&&!a._pf.nullInput&&!a._pf.invalidFormat&&!a._pf.userInvalidated,a._strict&&
(a._isValid=a._isValid&&0===a._pf.charsLeftOver&&0===a._pf.unusedTokens.length));return a._isValid}function L(a){return a?a.toLowerCase().replace("_","-"):a}function M(a,b){return b._isUTC?e(a).zone(b._offset||0):e(a).local()}function s(a){var b=0,c,d,f,g,h=function(a){if(!y[a]&&ea)try{require("./lang/"+a)}catch(b){}return y[a]};if(!a)return e.fn._lang;if(!J(a)){if(d=h(a))return d;a=[a]}for(;b<a.length;){g=L(a[b]).split("-");c=g.length;for(f=(f=L(a[b+1]))?f.split("-"):null;0<c;){if(d=h(g.slice(0,
c).join("-")))return d;if(f&&f.length>=c&&ca(g,f,!0)>=c-1)break;c--}b++}return e.fn._lang}function xa(a){return a.match(/\[[\s\S]/)?a.replace(/^\[|\]$/g,""):a.replace(/\\/g,"")}function ya(a){var b=a.match(fa),c,d;c=0;for(d=b.length;c<d;c++)b[c]=u[b[c]]?u[b[c]]:xa(b[c]);return function(f){var e="";for(c=0;c<d;c++)e+=b[c]instanceof Function?b[c].call(f,a):b[c];return e}}function N(a,b){if(!a.isValid())return a.lang().invalidDate();b=ga(b,a.lang());O[b]||(O[b]=ya(b));return O[b](a)}function ga(a,b){function c(a){return b.longDateFormat(a)||
a}var d=5;for(D.lastIndex=0;0<=d&&D.test(a);)a=a.replace(D,c),D.lastIndex=0,d-=1;return a}function za(a,b){var c=b._strict;switch(a){case "DDDD":return ha;case "YYYY":case "GGGG":case "gggg":return c?Aa:Ba;case "Y":case "G":case "g":return Ca;case "YYYYYY":case "YYYYY":case "GGGGG":case "ggggg":return c?Da:Ea;case "S":if(c)return Fa;case "SS":if(c)return ia;case "SSS":if(c)return ha;case "DDD":return Ga;case "MMM":case "MMMM":case "dd":case "ddd":case "dddd":return Ha;case "a":case "A":return s(b._l)._meridiemParse;
case "X":return Ia;case "Z":case "ZZ":return P;case "T":return Ja;case "SSSS":return Ka;case "MM":case "DD":case "YY":case "GG":case "gg":case "HH":case "hh":case "mm":case "ss":case "ww":case "WW":return c?ia:ja;case "M":case "D":case "d":case "H":case "h":case "m":case "s":case "w":case "W":case "e":case "E":return ja;default:var c=RegExp,d;d=La(a.replace("\\","")).replace(/[-\/\\^$*+?.()|[\]{}]/g,"\\$&");return new c(d)}}function ka(a){a=(a||"").match(P)||[];a=((a[a.length-1]||[])+"").match(Ma)||
["-",0,0];var b=+(60*a[1])+k(a[2]);return"+"===a[0]?-b:b}function Q(a){var b,c=[],d,f,g,h,l;if(!a._d){d=Na(a);a._w&&null==a._a[t]&&null==a._a[w]&&(b=function(b){var c=parseInt(b,10);return b?3>b.length?68<c?1900+c:2E3+c:c:null==a._a[r]?e().weekYear():a._a[r]},f=a._w,null!=f.GG||null!=f.W||null!=f.E?b=la(b(f.GG),f.W||1,f.E,4,1):(g=s(a._l),h=null!=f.d?ma(f.d,g):null!=f.e?parseInt(f.e,10)+g._week.dow:0,l=parseInt(f.w,10)||1,null!=f.d&&h<g._week.dow&&l++,b=la(b(f.gg),l,h,g._week.doy,g._week.dow)),a._a[r]=
b.year,a._dayOfYear=b.dayOfYear);a._dayOfYear&&(b=null==a._a[r]?d[r]:a._a[r],a._dayOfYear>(K(b)?366:365)&&(a._pf._overflowDayOfYear=!0),b=R(b,0,a._dayOfYear),a._a[w]=b.getUTCMonth(),a._a[t]=b.getUTCDate());for(b=0;3>b&&null==a._a[b];++b)a._a[b]=c[b]=d[b];for(;7>b;b++)a._a[b]=c[b]=null==a._a[b]?2===b?1:0:a._a[b];c[q]+=k((a._tzm||0)/60);c[x]+=k((a._tzm||0)%60);a._d=(a._useUTC?R:Oa).apply(null,c)}}function Na(a){var b=new Date;return a._useUTC?[b.getUTCFullYear(),b.getUTCMonth(),b.getUTCDate()]:[b.getFullYear(),
b.getMonth(),b.getDate()]}function S(a){a._a=[];a._pf.empty=!0;var b=s(a._l),c=""+a._i,d,f,e,h,l=c.length,m=0;f=ga(a._f,b).match(fa)||[];for(b=0;b<f.length;b++){e=f[b];if(d=(c.match(za(e,a))||[])[0])h=c.substr(0,c.indexOf(d)),0<h.length&&a._pf.unusedInput.push(h),c=c.slice(c.indexOf(d)+d.length),m+=d.length;if(u[e]){d?a._pf.empty=!1:a._pf.unusedTokens.push(e);h=a;var p=void 0,n=h._a;switch(e){case "M":case "MM":null!=d&&(n[w]=k(d)-1);break;case "MMM":case "MMMM":p=s(h._l).monthsParse(d);null!=p?n[w]=
p:h._pf.invalidMonth=d;break;case "D":case "DD":null!=d&&(n[t]=k(d));break;case "DDD":case "DDDD":null!=d&&(h._dayOfYear=k(d));break;case "YY":n[r]=k(d)+(68<k(d)?1900:2E3);break;case "YYYY":case "YYYYY":case "YYYYYY":n[r]=k(d);break;case "a":case "A":h._isPm=s(h._l).isPM(d);break;case "H":case "HH":case "h":case "hh":n[q]=k(d);break;case "m":case "mm":n[x]=k(d);break;case "s":case "ss":n[B]=k(d);break;case "S":case "SS":case "SSS":case "SSSS":n[C]=k(1E3*("0."+d));break;case "X":h._d=new Date(1E3*
parseFloat(d));break;case "Z":case "ZZ":h._useUTC=!0;h._tzm=ka(d);break;case "w":case "ww":case "W":case "WW":case "d":case "dd":case "ddd":case "dddd":case "e":case "E":e=e.substr(0,1);case "gg":case "gggg":case "GG":case "GGGG":case "GGGGG":e=e.substr(0,2),d&&(h._w=h._w||{},h._w[e]=d)}}else a._strict&&!d&&a._pf.unusedTokens.push(e)}a._pf.charsLeftOver=l-m;0<c.length&&a._pf.unusedInput.push(c);a._isPm&&12>a._a[q]&&(a._a[q]+=12);!1===a._isPm&&12===a._a[q]&&(a._a[q]=0);Q(a);aa(a)}function La(a){return a.replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g,
function(a,c,d,f,e){return c||d||f||e})}function Oa(a,b,c,d,f,e,h){b=new Date(a,b,c,d,f,e,h);1970>a&&b.setFullYear(a);return b}function R(a){var b=new Date(Date.UTC.apply(null,arguments));1970>a&&b.setUTCFullYear(a);return b}function ma(a,b){if("string"===typeof a)if(isNaN(a)){if(a=b.weekdaysParse(a),"number"!==typeof a)return null}else a=parseInt(a,10);return a}function Pa(a,b,c,d,e){return e.relativeTime(b||1,!!c,a,d)}function E(a,b,c){b=c-b;c-=a.day();c>b&&(c-=7);c<b-7&&(c+=7);a=e(a).add("d",c);
return{week:Math.ceil(a.dayOfYear()/7),year:a.year()}}function la(a,b,c,d,e){var g=R(a,0,1).getUTCDay();b=7*(b-1)+((null!=c?c:e)-e)+(e-g+(g>d?7:0)-(g<e?7:0))+1;return{year:0<b?a:a-1,dayOfYear:0<b?b:(K(a-1)?366:365)+b}}function na(a){var b=a._i,c=a._f;if(null===b)return e.invalid({nullInput:!0});"string"===typeof b&&(a._i=b=s().preparse(b));if(e.isMoment(b)){a=b;var d={},f;for(f in a)a.hasOwnProperty(f)&&Qa.hasOwnProperty(f)&&(d[f]=a[f]);a=d;a._d=new Date(+b._d)}else if(c)if(J(c)){var b=a,g,h;if(0===
b._f.length)b._pf.invalidFormat=!0,b._d=new Date(NaN);else{for(f=0;f<b._f.length;f++)if(c=0,d=p({},b),d._pf=F(),d._f=b._f[f],S(d),da(d)&&(c+=d._pf.charsLeftOver,c+=10*d._pf.unusedTokens.length,d._pf.score=c,null==h||c<h))h=c,g=d;p(b,g||d)}}else S(a);else if(d=a,g=d._i,h=Ra.exec(g),g===A)d._d=new Date;else if(h)d._d=new Date(+h[1]);else if("string"===typeof g)if(b=d._i,f=Sa.exec(b)){d._pf.iso=!0;g=0;for(h=T.length;g<h;g++)if(T[g][1].exec(b)){d._f=T[g][0]+(f[6]||" ");break}g=0;for(h=U.length;g<h;g++)if(U[g][1].exec(b)){d._f+=
U[g][0];break}b.match(P)&&(d._f+="Z");S(d)}else d._d=new Date(b);else J(g)?(d._a=g.slice(0),Q(d)):"[object Date]"===Object.prototype.toString.call(g)||g instanceof Date?d._d=new Date(+g):"object"===typeof g?d._d||(g=ba(d._i),d._a=[g.year,g.month,g.day,g.hour,g.minute,g.second,g.millisecond],Q(d)):d._d=new Date(g);return new G(a)}function oa(a,b){e.fn[a]=e.fn[a+"s"]=function(a){var d=this._isUTC?"UTC":"";return null!=a?(this._d["set"+d+b](a),e.updateOffset(this),this):this._d["get"+d+b]()}}function Ta(a){e.duration.fn[a]=
function(){return this._data[a]}}function pa(a,b){e.duration.fn["as"+a]=function(){return+this/b}}function V(a){var b=!1,c=e;"undefined"===typeof ender&&(a?(W.moment=function(){!b&&console&&console.warn&&(b=!0,console.warn("Accessing Moment through the global scope is deprecated, and will be removed in an upcoming release."));return c.apply(null,arguments)},p(W.moment,c)):W.moment=e)}for(var e,W=this,z=Math.round,l,r=0,w=1,t=2,q=3,x=4,B=5,C=6,y={},Qa={_isAMomentObject:null,_i:null,_f:null,_l:null,
_strict:null,_isUTC:null,_offset:null,_pf:null,_lang:null},ea="undefined"!==typeof module&&module.exports&&"undefined"!==typeof require,Ra=/^\/?Date\((\-?\d+)/i,Ua=/(\-)?(?:(\d*)\.)?(\d+)\:(\d+)(?:\:(\d+)\.?(\d{3})?)?/,Va=/^(-)?P(?:(?:([0-9,.]*)Y)?(?:([0-9,.]*)M)?(?:([0-9,.]*)D)?(?:T(?:([0-9,.]*)H)?(?:([0-9,.]*)M)?(?:([0-9,.]*)S)?)?|([0-9,.]*)W)$/,fa=/(\[[^\[]*\])|(\\)?(Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|mm?|ss?|S{1,4}|X|zz?|ZZ?|.)/g,
D=/(\[[^\[]*\])|(\\)?(LT|LL?L?L?|l{1,4})/g,ja=/\d\d?/,Ga=/\d{1,3}/,Ba=/\d{1,4}/,Ea=/[+\-]?\d{1,6}/,Ka=/\d+/,Ha=/[0-9]*['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF]+|[\u0600-\u06FF\/]+(\s*?[\u0600-\u06FF]+){1,2}/i,P=/Z|[\+\-]\d\d:?\d\d/gi,Ja=/T/i,Ia=/[\+\-]?\d+(\.\d{1,3})?/,Fa=/\d/,ia=/\d\d/,ha=/\d{3}/,Aa=/\d{4}/,Da=/[+-]?\d{6}/,Ca=/[+-]?\d+/,Sa=/^\s*(?:[+-]\d{6}|\d{4})-(?:(\d\d-\d\d)|(W\d\d$)|(W\d\d-\d)|(\d\d\d))((T| )(\d\d(:\d\d(:\d\d(\.\d+)?)?)?)?([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/,T=
[["YYYYYY-MM-DD",/[+-]\d{6}-\d{2}-\d{2}/],["YYYY-MM-DD",/\d{4}-\d{2}-\d{2}/],["GGGG-[W]WW-E",/\d{4}-W\d{2}-\d/],["GGGG-[W]WW",/\d{4}-W\d{2}/],["YYYY-DDD",/\d{4}-\d{3}/]],U=[["HH:mm:ss.SSSS",/(T| )\d\d:\d\d:\d\d\.\d{1,3}/],["HH:mm:ss",/(T| )\d\d:\d\d:\d\d/],["HH:mm",/(T| )\d\d:\d\d/],["HH",/(T| )\d\d/]],Ma=/([\+\-]|\d\d)/gi,X=["Date","Hours","Minutes","Seconds","Milliseconds"],Y={Milliseconds:1,Seconds:1E3,Minutes:6E4,Hours:36E5,Days:864E5,Months:2592E6,Years:31536E6},ua={ms:"millisecond",s:"second",
m:"minute",h:"hour",d:"day",D:"date",w:"week",W:"isoWeek",M:"month",y:"year",DDD:"dayOfYear",e:"weekday",E:"isoWeekday",gg:"weekYear",GG:"isoWeekYear"},va={dayofyear:"dayOfYear",isoweekday:"isoWeekday",isoweek:"isoWeek",weekyear:"weekYear",isoweekyear:"isoWeekYear"},O={},qa="DDD w W M D d".split(" "),ra="MDHhmswW".split(""),u={M:function(){return this.month()+1},MMM:function(a){return this.lang().monthsShort(this,a)},MMMM:function(a){return this.lang().months(this,a)},D:function(){return this.date()},
DDD:function(){return this.dayOfYear()},d:function(){return this.day()},dd:function(a){return this.lang().weekdaysMin(this,a)},ddd:function(a){return this.lang().weekdaysShort(this,a)},dddd:function(a){return this.lang().weekdays(this,a)},w:function(){return this.week()},W:function(){return this.isoWeek()},YY:function(){return m(this.year()%100,2)},YYYY:function(){return m(this.year(),4)},YYYYY:function(){return m(this.year(),5)},YYYYYY:function(){var a=this.year();return(0<=a?"+":"-")+m(Math.abs(a),
6)},gg:function(){return m(this.weekYear()%100,2)},gggg:function(){return m(this.weekYear(),4)},ggggg:function(){return m(this.weekYear(),5)},GG:function(){return m(this.isoWeekYear()%100,2)},GGGG:function(){return m(this.isoWeekYear(),4)},GGGGG:function(){return m(this.isoWeekYear(),5)},e:function(){return this.weekday()},E:function(){return this.isoWeekday()},a:function(){return this.lang().meridiem(this.hours(),this.minutes(),!0)},A:function(){return this.lang().meridiem(this.hours(),this.minutes(),
!1)},H:function(){return this.hours()},h:function(){return this.hours()%12||12},m:function(){return this.minutes()},s:function(){return this.seconds()},S:function(){return k(this.milliseconds()/100)},SS:function(){return m(k(this.milliseconds()/10),2)},SSS:function(){return m(this.milliseconds(),3)},SSSS:function(){return m(this.milliseconds(),3)},Z:function(){var a=-this.zone(),b="+";0>a&&(a=-a,b="-");return b+m(k(a/60),2)+":"+m(k(a)%60,2)},ZZ:function(){var a=-this.zone(),b="+";0>a&&(a=-a,b="-");
return b+m(k(a/60),2)+m(k(a)%60,2)},z:function(){return this.zoneAbbr()},zz:function(){return this.zoneName()},X:function(){return this.unix()},Q:function(){return this.quarter()}},sa=["months","monthsShort","weekdays","weekdaysShort","weekdaysMin"];qa.length;)l=qa.pop(),u[l+"o"]=ta(u[l],l);for(;ra.length;)l=ra.pop(),u[l+l]=Z(u[l],2);u.DDDD=Z(u.DDD,3);p($.prototype,{set:function(a){var b,c;for(c in a)b=a[c],"function"===typeof b?this[c]=b:this["_"+c]=b},_months:"January February March April May June July August September October November December".split(" "),
months:function(a){return this._months[a.month()]},_monthsShort:"Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(" "),monthsShort:function(a){return this._monthsShort[a.month()]},monthsParse:function(a){var b,c;this._monthsParse||(this._monthsParse=[]);for(b=0;12>b;b++)if(this._monthsParse[b]||(c=e.utc([2E3,b]),c="^"+this.months(c,"")+"|^"+this.monthsShort(c,""),this._monthsParse[b]=RegExp(c.replace(".",""),"i")),this._monthsParse[b].test(a))return b},_weekdays:"Sunday Monday Tuesday Wednesday Thursday Friday Saturday".split(" "),
weekdays:function(a){return this._weekdays[a.day()]},_weekdaysShort:"Sun Mon Tue Wed Thu Fri Sat".split(" "),weekdaysShort:function(a){return this._weekdaysShort[a.day()]},_weekdaysMin:"Su Mo Tu We Th Fr Sa".split(" "),weekdaysMin:function(a){return this._weekdaysMin[a.day()]},weekdaysParse:function(a){var b,c;this._weekdaysParse||(this._weekdaysParse=[]);for(b=0;7>b;b++)if(this._weekdaysParse[b]||(c=e([2E3,1]).day(b),c="^"+this.weekdays(c,"")+"|^"+this.weekdaysShort(c,"")+"|^"+this.weekdaysMin(c,
""),this._weekdaysParse[b]=RegExp(c.replace(".",""),"i")),this._weekdaysParse[b].test(a))return b},_longDateFormat:{LT:"h:mm A",L:"MM/DD/YYYY",LL:"MMMM D YYYY",LLL:"MMMM D YYYY LT",LLLL:"dddd, MMMM D YYYY LT"},longDateFormat:function(a){var b=this._longDateFormat[a];!b&&this._longDateFormat[a.toUpperCase()]&&(b=this._longDateFormat[a.toUpperCase()].replace(/MMMM|MM|DD|dddd/g,function(a){return a.slice(1)}),this._longDateFormat[a]=b);return b},isPM:function(a){return"p"===(a+"").toLowerCase().charAt(0)},
_meridiemParse:/[ap]\.?m?\.?/i,meridiem:function(a,b,c){return 11<a?c?"pm":"PM":c?"am":"AM"},_calendar:{sameDay:"[Today at] LT",nextDay:"[Tomorrow at] LT",nextWeek:"dddd [at] LT",lastDay:"[Yesterday at] LT",lastWeek:"[Last] dddd [at] LT",sameElse:"L"},calendar:function(a,b){var c=this._calendar[a];return"function"===typeof c?c.apply(b):c},_relativeTime:{future:"in %s",past:"%s ago",s:"a few seconds",m:"a minute",mm:"%d minutes",h:"an hour",hh:"%d hours",d:"a day",dd:"%d days",M:"a month",MM:"%d months",
y:"a year",yy:"%d years"},relativeTime:function(a,b,c,d){var e=this._relativeTime[c];return"function"===typeof e?e(a,b,c,d):e.replace(/%d/i,a)},pastFuture:function(a,b){var c=this._relativeTime[0<a?"future":"past"];return"function"===typeof c?c(b):c.replace(/%s/i,b)},ordinal:function(a){return this._ordinal.replace("%d",a)},_ordinal:"%d",preparse:function(a){return a},postformat:function(a){return a},week:function(a){return E(a,this._week.dow,this._week.doy).week},_week:{dow:0,doy:6},_invalidDate:"Invalid date",
invalidDate:function(){return this._invalidDate}});e=function(a,b,c,d){var e;"boolean"===typeof c&&(d=c,c=A);e={_isAMomentObject:!0};e._i=a;e._f=b;e._l=c;e._strict=d;e._isUTC=!1;e._pf=F();return na(e)};e.utc=function(a,b,c,d){var e;"boolean"===typeof c&&(d=c,c=A);e={_isAMomentObject:!0,_useUTC:!0,_isUTC:!0};e._l=c;e._i=a;e._f=b;e._strict=d;e._pf=F();return na(e).utc()};e.unix=function(a){return e(1E3*a)};e.duration=function(a,b){var c=a,d=null,f;if(e.isDuration(a))c={ms:a._milliseconds,d:a._days,
M:a._months};else if("number"===typeof a)c={},b?c[b]=a:c.milliseconds=a;else if(d=Ua.exec(a))f="-"===d[1]?-1:1,c={y:0,d:k(d[t])*f,h:k(d[q])*f,m:k(d[x])*f,s:k(d[B])*f,ms:k(d[C])*f};else if(d=Va.exec(a))f="-"===d[1]?-1:1,c=function(a){a=a&&parseFloat(a.replace(",","."));return(isNaN(a)?0:a)*f},c={y:c(d[2]),M:c(d[3]),d:c(d[4]),h:c(d[5]),m:c(d[6]),s:c(d[7]),w:c(d[8])};d=new H(c);e.isDuration(a)&&a.hasOwnProperty("_lang")&&(d._lang=a._lang);return d};e.version="2.5.1";e.defaultFormat="YYYY-MM-DDTHH:mm:ssZ";
e.updateOffset=function(){};e.lang=function(a,b){if(!a)return e.fn._lang._abbr;if(b){var c=L(a);b.abbr=c;y[c]||(y[c]=new $);y[c].set(b)}else null===b?(delete y[a],a="en"):y[a]||s(a);return(e.duration.fn._lang=e.fn._lang=s(a))._abbr};e.langData=function(a){a&&a._lang&&a._lang._abbr&&(a=a._lang._abbr);return s(a)};e.isMoment=function(a){return a instanceof G||null!=a&&a.hasOwnProperty("_isAMomentObject")};e.isDuration=function(a){return a instanceof H};for(l=sa.length-1;0<=l;--l)wa(sa[l]);e.normalizeUnits=
function(a){return n(a)};e.invalid=function(a){var b=e.utc(NaN);null!=a?p(b._pf,a):b._pf.userInvalidated=!0;return b};e.parseZone=function(a){return e(a).parseZone()};p(e.fn=G.prototype,{clone:function(){return e(this)},valueOf:function(){return+this._d+6E4*(this._offset||0)},unix:function(){return Math.floor(+this/1E3)},toString:function(){return this.clone().lang("en").format("ddd MMM DD YYYY HH:mm:ss [GMT]ZZ")},toDate:function(){return this._offset?new Date(+this):this._d},toISOString:function(){var a=
e(this).utc();return 0<a.year()&&9999>=a.year()?N(a,"YYYY-MM-DD[T]HH:mm:ss.SSS[Z]"):N(a,"YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]")},toArray:function(){return[this.year(),this.month(),this.date(),this.hours(),this.minutes(),this.seconds(),this.milliseconds()]},isValid:function(){return da(this)},isDSTShifted:function(){return this._a?this.isValid()&&0<ca(this._a,(this._isUTC?e.utc(this._a):e(this._a)).toArray()):!1},parsingFlags:function(){return p({},this._pf)},invalidAt:function(){return this._pf.overflow},
utc:function(){return this.zone(0)},local:function(){this.zone(0);this._isUTC=!1;return this},format:function(a){a=N(this,a||e.defaultFormat);return this.lang().postformat(a)},add:function(a,b){var c;c="string"===typeof a?e.duration(+b,a):e.duration(a,b);I(this,c,1);return this},subtract:function(a,b){var c;c="string"===typeof a?e.duration(+b,a):e.duration(a,b);I(this,c,-1);return this},diff:function(a,b,c){a=M(a,this);var d=6E4*(this.zone()-a.zone()),f;b=n(b);"year"===b||"month"===b?(f=432E5*(this.daysInMonth()+
a.daysInMonth()),d=12*(this.year()-a.year())+(this.month()-a.month()),d+=(this-e(this).startOf("month")-(a-e(a).startOf("month")))/f,d-=6E4*(this.zone()-e(this).startOf("month").zone()-(a.zone()-e(a).startOf("month").zone()))/f,"year"===b&&(d/=12)):(f=this-a,d="second"===b?f/1E3:"minute"===b?f/6E4:"hour"===b?f/36E5:"day"===b?(f-d)/864E5:"week"===b?(f-d)/6048E5:f);return c?d:v(d)},from:function(a,b){return e.duration(this.diff(a)).lang(this.lang()._abbr).humanize(!b)},fromNow:function(a){return this.from(e(),
a)},calendar:function(){var a=M(e(),this).startOf("day"),a=this.diff(a,"days",!0),a=-6>a?"sameElse":-1>a?"lastWeek":0>a?"lastDay":1>a?"sameDay":2>a?"nextDay":7>a?"nextWeek":"sameElse";return this.format(this.lang().calendar(a,this))},isLeapYear:function(){return K(this.year())},isDST:function(){return this.zone()<this.clone().month(0).zone()||this.zone()<this.clone().month(5).zone()},day:function(a){var b=this._isUTC?this._d.getUTCDay():this._d.getDay();return null!=a?(a=ma(a,this.lang()),this.add({d:a-
b})):b},month:function(a){var b=this._isUTC?"UTC":"",c;if(null!=a){if("string"===typeof a&&(a=this.lang().monthsParse(a),"number"!==typeof a))return this;c=this.date();this.date(1);this._d["set"+b+"Month"](a);this.date(Math.min(c,this.daysInMonth()));e.updateOffset(this);return this}return this._d["get"+b+"Month"]()},startOf:function(a){a=n(a);switch(a){case "year":this.month(0);case "month":this.date(1);case "week":case "isoWeek":case "day":this.hours(0);case "hour":this.minutes(0);case "minute":this.seconds(0);
case "second":this.milliseconds(0)}"week"===a?this.weekday(0):"isoWeek"===a&&this.isoWeekday(1);return this},endOf:function(a){a=n(a);return this.startOf(a).add("isoWeek"===a?"week":a,1).subtract("ms",1)},isAfter:function(a,b){b="undefined"!==typeof b?b:"millisecond";return+this.clone().startOf(b)>+e(a).startOf(b)},isBefore:function(a,b){b="undefined"!==typeof b?b:"millisecond";return+this.clone().startOf(b)<+e(a).startOf(b)},isSame:function(a,b){b=b||"ms";return+this.clone().startOf(b)===+M(a,this).startOf(b)},
min:function(a){a=e.apply(null,arguments);return a<this?this:a},max:function(a){a=e.apply(null,arguments);return a>this?this:a},zone:function(a){var b=this._offset||0;if(null!=a)"string"===typeof a&&(a=ka(a)),16>Math.abs(a)&&(a*=60),this._offset=a,this._isUTC=!0,b!==a&&I(this,e.duration(b-a,"m"),1,!0);else return this._isUTC?b:this._d.getTimezoneOffset();return this},zoneAbbr:function(){return this._isUTC?"UTC":""},zoneName:function(){return this._isUTC?"Coordinated Universal Time":""},parseZone:function(){this._tzm?
this.zone(this._tzm):"string"===typeof this._i&&this.zone(this._i);return this},hasAlignedHourOffset:function(a){a=a?e(a).zone():0;return 0===(this.zone()-a)%60},daysInMonth:function(){var a=this.year(),b=this.month();return(new Date(Date.UTC(a,b+1,0))).getUTCDate()},dayOfYear:function(a){var b=z((e(this).startOf("day")-e(this).startOf("year"))/864E5)+1;return null==a?b:this.add("d",a-b)},quarter:function(){return Math.ceil((this.month()+1)/3)},weekYear:function(a){var b=E(this,this.lang()._week.dow,
this.lang()._week.doy).year;return null==a?b:this.add("y",a-b)},isoWeekYear:function(a){var b=E(this,1,4).year;return null==a?b:this.add("y",a-b)},week:function(a){var b=this.lang().week(this);return null==a?b:this.add("d",7*(a-b))},isoWeek:function(a){var b=E(this,1,4).week;return null==a?b:this.add("d",7*(a-b))},weekday:function(a){var b=(this.day()+7-this.lang()._week.dow)%7;return null==a?b:this.add("d",a-b)},isoWeekday:function(a){return null==a?this.day()||7:this.day(this.day()%7?a:a-7)},get:function(a){a=
n(a);return this[a]()},set:function(a,b){a=n(a);if("function"===typeof this[a])this[a](b);return this},lang:function(a){if(a===A)return this._lang;this._lang=s(a);return this}});for(l=0;l<X.length;l++)oa(X[l].toLowerCase().replace(/s$/,""),X[l]);oa("year","FullYear");e.fn.days=e.fn.day;e.fn.months=e.fn.month;e.fn.weeks=e.fn.week;e.fn.isoWeeks=e.fn.isoWeek;e.fn.toJSON=e.fn.toISOString;p(e.duration.fn=H.prototype,{_bubble:function(){var a=this._milliseconds,b=this._days,c=this._months,d=this._data;
d.milliseconds=a%1E3;a=v(a/1E3);d.seconds=a%60;a=v(a/60);d.minutes=a%60;a=v(a/60);d.hours=a%24;b+=v(a/24);d.days=b%30;c+=v(b/30);d.months=c%12;b=v(c/12);d.years=b},weeks:function(){return v(this.days()/7)},valueOf:function(){return this._milliseconds+864E5*this._days+this._months%12*2592E6+31536E6*k(this._months/12)},humanize:function(a){var b=+this,c;c=!a;var d=this.lang(),e=z(Math.abs(b)/1E3),g=z(e/60),h=z(g/60),k=z(h/24),l=z(k/365),e=45>e&&["s",e]||1===g&&["m"]||45>g&&["mm",g]||1===h&&["h"]||22>
h&&["hh",h]||1===k&&["d"]||25>=k&&["dd",k]||45>=k&&["M"]||345>k&&["MM",z(k/30)]||1===l&&["y"]||["yy",l];e[2]=c;e[3]=0<b;e[4]=d;c=Pa.apply({},e);a&&(c=this.lang().pastFuture(b,c));return this.lang().postformat(c)},add:function(a,b){var c=e.duration(a,b);this._milliseconds+=c._milliseconds;this._days+=c._days;this._months+=c._months;this._bubble();return this},subtract:function(a,b){var c=e.duration(a,b);this._milliseconds-=c._milliseconds;this._days-=c._days;this._months-=c._months;this._bubble();
return this},get:function(a){a=n(a);return this[a.toLowerCase()+"s"]()},as:function(a){a=n(a);return this["as"+a.charAt(0).toUpperCase()+a.slice(1)+"s"]()},lang:e.fn.lang,toIsoString:function(){var a=Math.abs(this.years()),b=Math.abs(this.months()),c=Math.abs(this.days()),d=Math.abs(this.hours()),e=Math.abs(this.minutes()),g=Math.abs(this.seconds()+this.milliseconds()/1E3);return this.asSeconds()?(0>this.asSeconds()?"-":"")+"P"+(a?a+"Y":"")+(b?b+"M":"")+(c?c+"D":"")+(d||e||g?"T":"")+(d?d+"H":"")+
(e?e+"M":"")+(g?g+"S":""):"P0D"}});for(l in Y)Y.hasOwnProperty(l)&&(pa(l,Y[l]),Ta(l.toLowerCase()));pa("Weeks",6048E5);e.duration.fn.asMonths=function(){return(+this-31536E6*this.years())/2592E6+12*this.years()};e.lang("en",{ordinal:function(a){var b=a%10,b=1===k(a%100/10)?"th":1===b?"st":2===b?"nd":3===b?"rd":"th";return a+b}});ea?(module.exports=e,V(!0)):"function"===typeof define&&define.amd?define("moment",function(a,b,c){c.config&&c.config()&&!0!==c.config().noGlobal&&V(c.config().noGlobal===
A);return e}):V()}).call(this);
