xquery version "1.0-ml";

(: create cpox load app-server :)
import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy";
declare variable $C := admin:get-configuration();
declare variable $NAME := 'MarkMail-Load';
declare variable $ID := admin:appserver-get-id($C, (), $NAME);
declare variable $THREADS := 30;
xdmp:set(
$C,
admin:xdbc-server-create(
$C,
admin:group-get-id($C, 'Default'),
$NAME,
'/usr/local/markmail/xquery/load',
8010,
0,
admin:database-get-id($C, 'MarkMail') ) )
,xdmp:set(
$C,
admin:appserver-set-threads(
$C, $ID, $THREADS) )
,admin:save-configuration($C)
