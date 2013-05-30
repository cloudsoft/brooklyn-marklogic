xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $FOREST as xs:string := xdmp:get-request-field('forest', '');
admin:forest-get-id(admin:get-configuration(), $FOREST)