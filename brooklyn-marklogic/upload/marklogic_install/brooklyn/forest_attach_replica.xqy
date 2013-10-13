xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
  at "/MarkLogic/admin.xqy";

declare variable $fname as xs:string := xdmp:get-request-field('fname', '');

declare variable $repname as xs:string := xdmp:get-request-field('repname', '');

declare variable $config := admin:get-configuration();

declare variable $forestId := admin:forest-get-id($config, $fname);

declare variable $replicaForestId := admin:forest-get-id($config, $repname);

declare variable $c1 := admin:forest-set-failover-enable($config, $forestId, fn:true());

declare variable $c2 := admin:forest-add-replica($c1, $forestId, $replicaForestId);

admin:save-configuration($c2)