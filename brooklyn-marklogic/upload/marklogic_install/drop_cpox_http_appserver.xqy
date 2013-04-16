xquery version "1.0-ml";

(: drop cpox load app-server :)
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy";
 
declare variable $C := admin:get-configuration();
declare variable $NAME := '8007-cpox';
declare variable $ID := admin:appserver-get-id($C, (), $NAME);
 
xdmp:set( 
$C,
admin:appserver-delete( $C, $ID))
,admin:save-configuration($C)

