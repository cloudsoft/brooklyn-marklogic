xquery version "1.0-ml";


(: Copyright 2002-2013 MarkLogic Corporation.  All Rights Reserved. :)

import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare namespace gr="http://marklogic.com/xdmp/group";


declare variable $config := admin:get-configuration();
declare variable $groupid := admin:group-get-id($config, "Default");
  xdmp:set( $config, admin:group-set-performance-metering-enabled($config,$groupid,true()))
, admin:save-configuration($config)
