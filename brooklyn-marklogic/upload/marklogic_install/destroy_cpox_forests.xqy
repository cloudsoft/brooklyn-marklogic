xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy";

declare variable $C := admin:get-configuration();


let $master-forest-ids := (
  for $forestId in xdmp:forests()
  let $forestName := xdmp:forest-name($forestId)
  return if (fn:starts-with($forestName, 'cpox') and fn:not(fn:ends-with($forestName, '-r'))) then $forestId else ()
)
for $id in $master-forest-ids
return xdmp:set($C, admin:forest-delete($C, $id, true()))
,
admin:save-configuration($C)

