xquery version "1.0-ml";

(: create database :)
 
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy" ;
 
declare variable $C as element() := admin:get-configuration();
declare variable $DBNAME as xs:string := xdmp:get-request-field('dbname', 'cpox');


let $db := xdmp:database($DBNAME)
return xdmp:set($C, admin:database-delete($C, $db))
,
admin:save-configuration($C)
