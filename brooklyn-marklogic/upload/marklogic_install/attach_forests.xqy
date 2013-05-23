xquery version "1.0-ml";


(: attach all cpox forests :)
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy";
 
declare variable $C := admin:get-configuration();
declare variable $NAME := 'cpox';
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');
declare variable $NODE as xs:string := xdmp:get-request-field('node', '');
declare variable $CLUSTER as xs:string := xdmp:get-request-field('cluster', '');



 
 
let $db := xdmp:database($NAME)
let $master-forest-ids := (
  for $forestId in xdmp:forests()
  let $forestName := xdmp:forest-name($forestId)
  let $prefix := fn:string-join(($CLUSTER, $NODE), '-')
  return if (fn:starts-with($forestName, $prefix) and fn:not(fn:contains($forestName, 'replica'))) then $forestId else ()
)
for $id in $master-forest-ids
return xdmp:set($C, admin:database-attach-forest($C, $db, $id))
, admin:save-configuration($C)

