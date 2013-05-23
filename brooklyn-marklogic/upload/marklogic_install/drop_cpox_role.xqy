xquery version "1.0-ml";

(: drop a role named "cpox" :)

import module namespace sec="http://marklogic.com/xdmp/security" at
    "/MarkLogic/security.xqy";
 
sec:remove-role("cpox")

