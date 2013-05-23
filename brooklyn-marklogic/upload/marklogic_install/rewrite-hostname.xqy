xquery version "0.9-ml"

(: Copyright 2002-2013 MarkLogic Corporation.  All Rights Reserved. :)

default element namespace = "http://www.w3.org/1999/xhtml"
declare namespace xs="http://www.w3.org/2001/XMLSchema"
declare namespace admin="http://marklogic.com/xdmp/admin"
declare namespace ho="http://marklogic.com/xdmp/hosts"
declare namespace gr="http://marklogic.com/xdmp/group"
declare namespace as="http://marklogic.com/xdmp/assignments"
declare namespace db="http://marklogic.com/xdmp/database"

import module "http://www.w3.org/2003/05/xpath-functions" at "lib/host-admin-form.xqy"
import module "http://www.w3.org/2003/05/xpath-functions" at "lib/session.xqy"

define function rebuildHosts(
$old-host as xs:string,
$new-host as xs:string)
as node()
{
  <hosts
    xsi:schemaLocation="http://marklogic.com/xdmp/hosts hosts.xsd"
    xmlns="http://marklogic.com/xdmp/hosts"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  {
    for $h in $fn:hosts.xml/ho:host
let $host-id := data($h/ho:host-id)
let $group := data($h/ho:group)
let $bind-port := data($h/ho:bind-port)
let $connect-port := data($h/ho:connect-port)
let $foreign-bind-port := data($h/ho:foreign-bind-port)
let $foreign-connect-port := data($h/ho:foreign-connect-port)
let $ssl-certificate := data($h/ho:ssl-certificate)
    return
      if (data($h/ho:host-name) = $old-host)
      then 
        <host>
          <host-id>{$host-id}</host-id>
          <host-name>{$new-host}</host-name>
          <group>{$group}</group>
          <bind-port>{$bind-port}</bind-port>
          <connect-port>{$connect-port}</connect-port>
          <foreign-bind-port>{$foreign-bind-port}</foreign-bind-port>
          <foreign-connect-port>{$foreign-connect-port}</foreign-connect-port>
          <ssl-certificate>{$ssl-certificate}</ssl-certificate>
        </host>
      else $h
  }
  </hosts>
}


let $old-hostname := xdmp:get-request-field("oldhost","") 
let $new-hostname := xdmp:get-request-field("newhost","") 

let $remHost := rebuildHosts($old-hostname, $new-hostname)
let $config := xdmp:write-cluster-config-file("hosts.xml", $remHost)
return
   restart($config, "group",("section","group"))


