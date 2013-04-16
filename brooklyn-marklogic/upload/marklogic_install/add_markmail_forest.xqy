
xquery version "1.0-ml";
(:
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :)

import module namespace admin = "http://marklogic.com/xdmp/admin"
  at "/MarkLogic/admin.xqy";
import module namespace functx = "http://www.functx.com" 
  at "/MarkLogic/functx/functx-1.0-nodoc-2007-01.xqy";

declare variable $DBNAME as xs:string := xdmp:get-request-field('dbname', 'cpox');
declare variable $DATADIR as xs:string := xdmp:get-request-field('datadir', '');
declare variable $FORESTCNT as xs:integer := xs:integer(xdmp:get-request-field('fcount', '4'));
declare variable $HOST as xs:string := xdmp:get-request-field('host', '');
declare variable $NODE as xs:string := xdmp:get-request-field('node', '');
declare variable $CLUSTER as xs:string := xdmp:get-request-field('cluster', '');


declare variable $C := admin:get-configuration();

let $base := $DBNAME
let $hosts := xdmp:hosts()
return
  for $h in $hosts
    let $i := $FORESTCNT
    let $prefix := fn:string-join(($CLUSTER, $NODE), '-')
    let $fname := fn:string-join(($prefix, xs:string($i)), '-')
    let $forestid := functx:pad-integer-to-length($i,xs:integer(3))
    let $forest_name := fn:string-join(('MarkMail', $forestid), '-')
    let $dirname := fn:string-join(($DATADIR,$fname),'/')
      return if ($HOST eq xdmp:host-name($h))
        then xdmp:set($C,
       admin:forest-create($C, $forest_name, $h, $dirname, (), ''))
      else 0
,
if (0) then $C else admin:save-configuration($C)

