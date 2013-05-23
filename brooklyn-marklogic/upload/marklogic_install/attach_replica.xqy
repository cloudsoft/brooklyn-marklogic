xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
  at "/MarkLogic/admin.xqy";

declare variable $DBNAME as xs:string := xdmp:get-request-field('dbname', 'cpox');
declare variable $fname as xs:string := xdmp:get-request-field('fname', '');
 
declare variable $repname as xs:string := xdmp:get-request-field('repname', '');

declare variable $C := admin:get-configuration();

let $base := $DBNAME
return (xdmp:set($C,
   admin:forest-add-replica($C, xdmp:forest($fname), xdmp:forest($repname)))
, xdmp:set($C,
 admin:forest-set-failover-enable($C,xdmp:forest($fname),true())))
, 
if (0) then $C else admin:save-configuration($C) 



