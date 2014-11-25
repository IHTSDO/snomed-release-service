/*
	/Users/rtu/Google Drive/kp-donation/kpdonation.sql
*/;
drop database if exists kpnlm;
create database kpnlm;
use kpnlm;

/* 
	create SNOMED tables
	1. concept
	2. description
	3. association refset
	
*/;

drop table if exists sctconcept;
create table sctconcept(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
definitionstatusid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_definitionstatusid(definitionstatusid)
) engine=myisam default charset=utf8;

drop table if exists sctdescription;
create table sctdescription(
id bigint not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
conceptid bigint not null,
languagecode varchar(2) not null,
typeid bigint not null,
term varchar(255) not null,
casesignificanceid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_conceptid(conceptid),
key idx_languagecode(languagecode),
key idx_typeid(typeid),
key idx_term(term),
key idx_casesignificanceid(casesignificanceid)
) engine=myisam default charset=utf8;


drop table if exists sctassociationrefset;
create table sctassociationrefset(
id varchar(36) not null,
effectivetime char(8) not null,
active char(1) not null,
moduleid bigint not null,
refsetid bigint not null,
referencedcomponentid bigint not null,
targetcomponentid bigint not null,
key idx_id(id),
key idx_effectivetime(effectivetime),
key idx_active(active),
key idx_moduleid(moduleid),
key idx_refsetid(refsetid),
key idx_referencedcomponentid(referencedcomponentid),
key idx_targetcomponentid(targetcomponentid)
) engine=myisam default charset=utf8;

load data local 
	infile '/Users/rtu/release/20140731/packaging/member-release/target/SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20140731.txt' 
	into table sctconcept
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '/Users/rtu/release/20140731/packaging/member-release/target/SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20140731.txt' 
	into table sctdescription
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

load data local 
	infile '/Users/rtu/release/20140731/packaging/member-release/target/SnomedCT_Release_INT_20140731/RF2Release/Snapshot/Refset/Content/der2_cRefset_AssociationReferenceSnapshot_INT_20140731.txt' 
	into table sctassociationrefset
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;


/* make an easy concept/description temp table*/;

drop table if exists sctcondesc;
create table sctcondesc(
	id bigint not null,
	active smallint not null,
	term varchar(255) not null,
	key idx_id(id)
	) engine=myisam default charset=utf8;
insert into sctcondesc
select a.id, a.active, b.term
from sctconcept a
join sctdescription b
on a.id = b.conceptid
and b.typeid = '900000000000003001'
and b.active = 1;


/*
	create and load "cardio" table from the files that were created from the KP spreadsheets
	
	the 'specialty' column is populated as follows:
	
	CAR = cardiology
	INJ = injuries
	MUS = musculoskeletal
	OPT = opthalmology
	ORT = orthopedic
	PED = pediatrics
	
*/;
 
drop table if exists kpdonation;
create table kpdonation(	
	id varchar(18) not null,
	term varchar(255) not null,
	specialty char(3) null,
	key idx_id(id)
) engine=myisam default charset=utf8;

/*
	...loading the kpdonation table for these files, which have been created from 
	the spreadsheets that contained the donated content...

/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Cardiology_1000119_20140818.txt
/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Pediatrics_1000119_20141009.txt
/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Orthopedics_1000119_20140731.txt
/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Ophthalmology_1000119_20140818.txt
/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Musculoskeletal_1000119_20140818.txt
/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Injuries_1000119_20140731.txt
*/;


load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Cardiology_1000119_20140818.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

update kpdonation set specialty = 'CAR' where specialty is null;
	
load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Pediatrics_1000119_20141009.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;
	
update kpdonation set specialty = 'PED' where specialty is null;

	
load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Orthopedics_1000119_20140731.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

update kpdonation set specialty = 'ORT' where specialty is null;

	
load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Ophthalmology_1000119_20140818.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

update kpdonation set specialty = 'OPT' where specialty is null;

	
load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Musculoskeletal_1000119_20140818.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;

update kpdonation set specialty = 'MUS' where specialty is null;
	
load data local 
	infile '/Users/rtu/Google Drive/kp-donation/SNOMEDCT_Injuries_1000119_20140731.txt' 
	into table kpdonation
	columns terminated by '\t' 
	lines terminated by '\r\n' 
	ignore 1 lines;					

update kpdonation set specialty = 'INJ' where specialty is null;

/*
select specialty, count(specialty) from kpdonation group by specialty;
 
CAR	879
INJ	5825
MUS	3710
OPT	2505
ORT	1088
PED	3792
*/;



/*
	...making a table of identifiers in the KP file that do not exist in SNOMED
	we will want to document this list in the tech preview of the baseline 

*/;
drop table if exists kpnoexist;
create table kpnoexist(
	id varchar(18) not null,
	term varchar(255) not null,
	specialty char(3) not null,
	key idx_id(id)
) engine=myisam default charset=utf8;

insert into kpnoexist 
select a.id, a.term, a.specialty
from kpdonation a
left join sctconcept b
on a.id = b.id 
where b.id is null ;



/* 
	select specialty, count(specialty)
	from kpnoexist
	group by specialty;
	
CAR	225
INJ	4739
MUS	3072
OPT	2092
ORT	986
PED	1613
*/;

===

drop table if exists kpinactive;
create table kpinactive(
	id varchar(18) not null,
	specialty char(3) not null,
	key idx_idspecialty(id, specialty)
) engine=myisam default charset=utf8;

insert into kpinactive
select a.id, a.specialty
from kpdonation a
join sctconcept b
on a.id = b.id
where b.active = 0;

/*
	select specialty, count(specialty)
	from kpinactive
	group by specialty;
CAR	4
INJ	5
MUS	4
PED	21
*/;


/*
	... selecting the possible replacements for concepts that have been inactivated. These
	will sbe clinically evaluated to determine whether to replace the inactive concepts, 
	and if so what the replacements will be. If the inactive concept will not be replaced, 
	we will remove them from the set.
	  
	these are the currently available historical associations:
	
900000000000530003|ALTERNATIVE association reference set (foundation metadata concept)
900000000000525002|MOVED FROM association reference set (foundation metadata concept)
900000000000524003|MOVED TO association reference set (foundation metadata concept)
900000000000523009|POSSIBLY EQUIVALENT TO association reference set (foundation metadata concept)
900000000000531004|REFERS TO concept association reference set (foundation metadata concept)
900000000000526001|REPLACED BY association reference set (foundation metadata concept)
900000000000527005|SAME AS association reference set (foundation metadata concept)
900000000000529008|SIMILAR TO association reference set (foundation metadata concept)
900000000000528000|WAS A association reference set (foundation metadata concept)


*/;
drop table if exists evalinactive;
create table evalinactive(
	specialty char(3) not null,
	oldid varchar(18) not null,
	oldterm varchar(255) not null,
	association varchar(255) not null,
	newid varchar(255) not null,
	newterm varchar(255) not null,
	key idx_oldid(oldid)
) engine=myisam default charset=utf8;

insert into evalinactive (specialty, oldid, oldterm, association, newid)
select 
	a.specialty, a.id, c.term, 
	case 
		when b.refsetid = 900000000000530003 then 'ALTERNATIVE'
		when b.refsetid = 900000000000525002 then 'MOVED FROM'
		when b.refsetid = 900000000000524003 then 'MOVED TO'
		when b.refsetid = 900000000000523009 then 'POSSIBLY EQUIVALENT TO'
		when b.refsetid = 900000000000531004 then 'REFERS TO'
		when b.refsetid = 900000000000526001 then 'REPLACED BY'
		when b.refsetid = 900000000000527005 then 'SAME AS'
		when b.refsetid = 900000000000529008 then 'SIMILAR TO'
		when b.refsetid = 900000000000528000 then 'WAS A'
		else 'NO ASSOCIATION AVAILABLE' end,
	b.targetcomponentid 
from kpinactive a
left join sctassociationrefset b
	on a.id = b.referencedcomponentid
	and b.active = 1
left join sctcondesc c
	on a.id = c.id
order by b.referencedcomponentid;

update evalinactive a
join sctcondesc b
on a.newid = b.id
set a.newterm = b.term
where a.newid = b.id; 

select 
	case 
		when specialty = 'CAR' then 'cardiology'
		when specialty = 'INJ' then 'injuries'
		when specialty = 'MUS' then 'musculoskeletal'
		when specialty = 'OPT' then 'opthalmology'
		when specialty = 'ORT' then 'orthopedic'
		when specialty = 'PED' then 'pediatrics'
		else 'ERROR' end,
	oldid, oldterm, association, newid, newterm
from evalinactive order by specialty;

/*
	select specialty, count(specialty)
	from evalinactive
	group by specialty;

CAR	5
INJ	7
MUS	5
PED	25
*/;


select
	case 
		when specialty = 'CAR' then 'cardiology'
		when specialty = 'INJ' then 'injuries'
		when specialty = 'MUS' then 'musculoskeletal'
		when specialty = 'OPT' then 'opthalmology'
		when specialty = 'ORT' then 'orthopedic'
		when specialty = 'PED' then 'pediatrics'
		else 'ERROR' end as specialty,
		id, term 
from kpnoexist
order by specialty;


/*
	select specialty, count(specialty)
	from kpnoexist
	group by specialty;

CAR	225
INJ	4739
MUS	3072
OPT	2092
ORT	986
PED	1613
*/;












