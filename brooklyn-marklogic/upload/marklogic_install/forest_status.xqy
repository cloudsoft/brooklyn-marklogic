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


let $prefix:= normalize-space(xdmp:get-request-field("prefix",""))
let $forest-ids := (
  for $forestId in xdmp:forests()
  let $forestName := xdmp:forest-name($forestId)
  return if (fn:starts-with($forestName, $prefix ) and fn:not(fn:ends-with($forestName, '-r'))) then $forestId else ()
)

let $forest-counts := $FOREST-COUNTS[ fs:forest-id = $forest-ids ] 
let $forest-status := $FOREST-STATUS[ fs:forest-id = $forest-ids ] 
let $stand-count := count($forest-status/fs:stands/fs:stand)


let $forest-open := count($FOREST-STATUS[ (fs:forest-id = $forest-ids) and (fs:state eq "open") ] )
let $forest-merging := $FOREST-STATUS[ (fs:forest-id = $forest-ids) and (fs:state eq "merging") ] 
(:  :)
(:  :)
(: let $docs := sum($forest-counts/fs:document-count) :)
(: let $merge-read-bytes := sum($forest-status/fs:merge-read-bytes) :)
let $merge-count := count($forest-status/fs:merges/fs:merge) 
(:  :)
(: return $forest-status :)
return concat ($forest-open,' ',$stand-count ,' ',$merge-count,' ',$prefix, codepoints-to-string(10));


