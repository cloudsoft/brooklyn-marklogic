xquery version "1.0-ml";
declare namespace an="http://marklogic.com/xdmp/assignments";
declare namespace db="http://marklogic.com/xdmp/database";
declare namespace fs="http://marklogic.com/xdmp/status/forest";
declare namespace gr="http://marklogic.com/xdmp/group";
declare namespace ho="http://marklogic.com/xdmp/hosts";
declare namespace hs="http://marklogic.com/xdmp/status/host";
declare namespace xh="http://www.w3.org/1999/xhtml";

declare variable $SUPPORT := ( 
    for $i in xdmp:forest-status(xdmp:forests()) return document { $i },
    for $i in xdmp:forest-counts(xdmp:forests()) return document { $i },
    for $i in ('databases', 'assignments') return xdmp:read-cluster-config-file(concat($i, '.xml')));

declare variable $ASSIGNMENTS as element(an:assignment)* :=
  ($SUPPORT/an:assignments)[1]/an:assignment
;

declare variable $DATABASES as element(db:database)* :=
  ($SUPPORT/db:databases)[1]/db:database
;
declare variable $FOREST-COUNTS as element(fs:forest-counts)* :=
  $SUPPORT/fs:forest-counts
;

declare variable $FOREST-STATUS as element(fs:forest-status)* :=
  $SUPPORT/fs:forest-status
;

let $dbname:= normalize-space(xdmp:get-request-field("dbname",""))

for $database in $DATABASES [db:database-name eq $dbname ] 
return concat($database/db:database-id,codepoints-to-string(10));
