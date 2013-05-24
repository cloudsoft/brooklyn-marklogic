xquery version "1.0-ml";

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $FOREST    := xdmp:get-request-field('forest', '');
declare variable $FOREST_ID := admin:forest-get-id(admin:get-configuration(), $FOREST);
xdmp:forest-status($FOREST_ID)
