xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $GROUP as xs:string := xdmp:get-request-field('group', '');
admin:group-get-id(admin:get-configuration(), $GROUP)