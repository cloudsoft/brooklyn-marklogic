xquery version "1.0-ml";

(: create cpox http server :)
import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $C := admin:get-configuration();
declare variable $NAME := '8007-cpox';
declare variable $ID := admin:appserver-get-id($C, (), $NAME);
declare variable $THREADS := 4 * xdmp:host-cpus() * xdmp:host-cores() ;
xdmp:set(
$C,
admin:http-server-create(
$C,
admin:group-get-id($C, 'Default'),
$NAME,
'/space/marklogic_scripts',
8007,
0,
admin:database-get-id($C, 'cpox') ) )
,xdmp:set(
$C,
admin:appserver-set-default-user(
$C, $ID, xdmp:get-request-user() ) )
,xdmp:set(
$C,
admin:appserver-set-authentication(
$C, $ID, 'application-level') )
,xdmp:set(
$C,
admin:appserver-set-threads(
$C, $ID, $THREADS) )
,admin:save-configuration($C)
