
xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
  at "/MarkLogic/admin.xqy";
import module namespace functx = "http://www.functx.com" 
  at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

declare variable $DBNAME as xs:string := xdmp:get-request-field('dbname', 'MarkMail');
declare variable $DATADIR as xs:string := xdmp:get-request-field('datadir', '');
declare variable $FORESTCNT as xs:integer := xs:integer(xdmp:get-request-field('fcount', '4'));
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');
declare variable $NODE as xs:string := xdmp:get-request-field('node', '');
declare variable $CLUSTER as xs:string := xdmp:get-request-field('cluster', '');


declare variable $C := admin:get-configuration();

let $hosts := xdmp:hosts()
let $master-per-host := $FORESTCNT
let $node_number := number(substring-after($NODE, 'node'))
let $prev := $node_number - 1
let $forest-start :=  $master-per-host * $prev
return
  for $h in $hosts
    for $i in 1 to $master-per-host
       let $forest-number := $forest-start + $i
       let $prefix := fn:string-join(($CLUSTER, $NODE), '-')
       let $fname := fn:string-join(($prefix, xs:string($i)), '-')
       let $forestid := functx:pad-integer-to-length($forest-number,xs:integer(3))
       let $forest_name := fn:string-join(('MarkMail', $forestid), '-')
       let $dirname := fn:string-join(($DATADIR,$fname),'/')
         return if ($HOST eq xdmp:host-name($h))
          then xdmp:set($C,
         admin:forest-create($C, $forest_name, $h, $dirname, (), ''))
         else 0
,
if (0) then $C else admin:save-configuration($C)

