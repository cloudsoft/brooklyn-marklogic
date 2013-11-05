xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";

declare variable $DATABASE := xdmp:get-request-field('database', '');
declare variable $PORT := xs:integer(xdmp:get-request-field('port', ''));
declare variable $GROUP := xdmp:get-request-field('group', '');
declare variable $NAME := xdmp:get-request-field('name', '');
declare variable $KIND := xdmp:get-request-field('kind', '');

let $config := admin:get-configuration()
let $groupId := admin:group-get-id($config, $GROUP)
let $databaseId := admin:database-get-id($config, $DATABASE)

let $newConfig :=
    if ($KIND eq "HTTP") then
        admin:http-server-create($config, $groupId, $NAME, "/", $PORT, 0, $databaseId)
    else if ($KIND eq "XDBC") then
        admin:xdbc-server-create($config, $groupId, $NAME, "/", $PORT, 0, $databaseId)
    else
        fn:error(xs:QName("ERROR"), fn:concat("Unknown app server kind", $KIND))

return admin:save-configuration($newConfig)
