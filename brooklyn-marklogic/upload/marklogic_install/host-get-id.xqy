xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');
admin:host-get-id(admin:get-configuration(), $HOST)