xquery version "1.0-ml";


(: attach a forest to a database :)
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy";

declare variable $C := admin:get-configuration();
declare variable $DATABASE := xdmp:get-request-field('database', '');
declare variable $FOREST as xs:string := xdmp:get-request-field('forest', '');

let $db := xdmp:database($DATABASE)
let $master-forest-ids := (
  for $forestId in xdmp:forests()
  let $forestName := xdmp:forest-name($forestId)
  return if (fn:ends-with($forestName, $FOREST)) then $forestId else ()
)
for $forest-id in $master-forest-ids
return xdmp:set($C, admin:database-attach-forest($C, $db, $forest-id))
, admin:save-configuration($C)

