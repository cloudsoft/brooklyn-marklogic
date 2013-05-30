xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $DATABASE as xs:string := xdmp:get-request-field('database', '');
admin:database-get-id(admin:get-configuration(), $DATABASE)