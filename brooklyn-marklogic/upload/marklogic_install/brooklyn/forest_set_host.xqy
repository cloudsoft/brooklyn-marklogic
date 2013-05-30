xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $FOREST as xs:string := xdmp:get-request-field('forest', '');
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');

declare variable $config := admin:forest-set-host(admin:get-configuration(), admin:forest-get-id(admin:get-configuration(), $FOREST),
                                          xdmp:host($HOST));
admin:save-configuration($config)


