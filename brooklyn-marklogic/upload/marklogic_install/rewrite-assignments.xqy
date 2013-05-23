xquery version "1.0-ml";

(: Copyright 2002-2013 MarkLogic Corporation.  All Rights Reserved. :)

declare namespace xs="http://www.w3.org/2001/XMLSchema";
declare namespace admin="http://marklogic.com/xdmp/admin";
declare namespace ho="http://marklogic.com/xdmp/hosts";
declare namespace gr="http://marklogic.com/xdmp/group";
declare namespace an="http://marklogic.com/xdmp/assignments";
declare namespace db="http://marklogic.com/xdmp/database";


import module "http://www.w3.org/2003/05/xpath-functions" at "lib/host-admin-form.xqy";
import module "http://www.w3.org/2003/05/xpath-functions" at "lib/session.xqy";

declare variable $HOSTS as element (ho:host)*  := (xdmp:read-cluster-config-file("hosts.xml")/ho:hosts/ho:host);
declare variable $ASSIGNMENTS as element (an:assignment)*  := (xdmp:read-cluster-config-file("assignments.xml")/an:assignments/an:assignment);


declare function local:rebuildAssignments(
  $old-host as xs:unsignedLong,
  $new-host as xs:unsignedLong,
  $old-fname as xs:string,
  $new-dirname as xs:string
  $new-fname as xs:string)
as node()
{
  <assignments
    xsi:schemaLocation="http://marklogic.com/xdmp/assignments assignments.xsd"
    xmlns="http://marklogic.com/xdmp/assignments"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  {
    for $a in $ASSIGNMENTS
let $enabled := data($a/an:enabled)
let $large-data-directory := data($a/an:large-data-directory)
let $fast-data-directory := data($a/an:fast-data-directory)
let $updates-allowed := data($a/an:updates-allowed)
let $rebalancer-enable := data($a/an:rebalancer-enable)
let $failover-enable := data($a/an:failover-enable)
let $forest-id := data($a/an:forest-id)
(: let $failover-hosts := data($a/an:failover-hosts)  :)
(: let $forest-backups := data($a/an:forest-backups)  :)
(: let $forest-replicas := data($a/an:forest-replicas)  :)
(: let $database-replication := data($a/an:database-replication)  :)
    return
      if ($a/an:forest-name = $old-fname)
      then
        <assignment>
          <forest-name>{$new-fname}</forest-name>
          <enabled>{$enabled}</enabled>
          <host>{$new-host}</host>
          <data-directory>/var/opt/mldata/{$new-dirname}</data-directory>
          <large-data-directory/>
          <fast-data-directory/>
          <updates-allowed>{$updates-allowed}</updates-allowed>
          <rebalancer-enable>{$rebalancer-enable}</rebalancer-enable>
          <failover-enable>{$failover-enable}</failover-enable>
          <failover-hosts/>
          <forest-id>{$forest-id}</forest-id>
          <forest-backups/>
          <forest-replicas/>
          <database-replication/>
        </assignment>
      else $a
  }
  </assignments>
};



let $old-hostname := xdmp:get-request-field("oldhost","")
let $new-hostname := xdmp:get-request-field("newhost","")
let $old-fname := xdmp:get-request-field("oldfname","")
let $new-fname := xdmp:get-request-field("newfname","")
let $new-dirname := xdmp:get-request-field("newdname","")

let $old-hostid as xs:unsignedLong* :=  if ($old-hostname ne "" )
      then $HOSTS [ ho:host-name = $old-hostname ]/ho:host-id
      else 0

let $new-hostid as xs:unsignedLong* :=  if ($new-hostname ne "" )
      then $HOSTS [ ho:host-name = $new-hostname ]/ho:host-id
      else 0



let $remAssign := local:rebuildAssignments($old-hostid, $new-hostid, $old-fname, $new-dirname, $new-fname) 
let $config := xdmp:write-cluster-config-file("assignments.xml", $remAssign) 
 return  
    xdmp:restart((), "to reload assignments.xml") 

