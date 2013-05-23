xquery version "0.9-ml"

(: Copyright 2002-2013 MarkLogic Corporation.  All Rights Reserved. :)

default element namespace ="http://www.w3.org/1999/xhtml"
declare namespace xs="http://www.w3.org/2001/XMLSchema"
declare namespace admin="http://marklogic.com/xdmp/admin"
declare namespace db="http://marklogic.com/xdmp/database"
declare namespace as="http://marklogic.com/xdmp/assignments"

import module "http://www.w3.org/2003/05/xpath-functions" at "lib/session.xqy"
import module namespace admin = "http://marklogic.com/xdmp/admin" at "/MarkLogic/admin.xqy"

declare variable $E               := xdmp:get-request-field('enabled', '');
declare variable $ENABLED         := fn:codepoint-equal('true', $E);
declare variable $FOREST          := xdmp:get-request-field('forest', '');
declare variable $FOREST_ID       := admin:forest-get-id(admin:get-configuration(), $FOREST);

declare variable $config          := admin:get-configuration();
declare variable $c               := admin:forest-set-enabled($config, $FOREST_ID, $ENABLED);
admin:save-configuration($c);
xdmp:sleep(1500);

(:

let $enabled  := xdmp:get-request-field("enabled","");
    $value    := if ($disabled)
                  then false()
                 else true()
return
  let $forest          := xs:unsignedLong(xdmp:get-request-field("forest")),

  return
    (
      admin:save-configuration($c),
      xdmp:sleep(1500),
      redirect("forest-admin.xqy",("section","forest"))
    )
:)