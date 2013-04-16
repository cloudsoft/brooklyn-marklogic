
xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
  at "/MarkLogic/admin.xqy";

declare variable $DBNAME as xs:string := xdmp:get-request-field('dbname', 'cpox');
declare variable $FASTDIR as xs:string := xdmp:get-request-field('fastdir', '');
declare variable $DATADIR as xs:string := xdmp:get-request-field('datadir', '');
declare variable $FORESTCNT as xs:integer := xs:integer(xdmp:get-request-field('fcount', '4'));
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');
declare variable $NODE as xs:string := xdmp:get-request-field('node', '');
declare variable $CLUSTER as xs:string := xdmp:get-request-field('cluster', '');


declare variable $C := admin:get-configuration();

let $base := $DBNAME
let $hosts := xdmp:hosts()
let $master-per-host := $FORESTCNT
return
  for $h in $hosts
    for $i in 1 to $master-per-host
       let $prefix := fn:string-join(($CLUSTER, $NODE), '-')
       let $fname := fn:string-join(($prefix, xs:string($i)), '-')
       let $fast-fname := fn:string-join(($prefix, 'fastdir',  xs:string($i)), '-')
       let $dirname := fn:string-join(($DATADIR,$fname),'/')
       let $fastdirname := fn:string-join(($FASTDIR,$fast-fname),'/')
         return if ($HOST eq xdmp:host-name($h))
          then xdmp:set($C,
         admin:forest-create($C, $fname, $h, $dirname, (), $fastdirname))
         else 0
,
if (0) then $C else admin:save-configuration($C)

