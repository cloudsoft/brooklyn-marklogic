xquery version "1.0-ml";

(: create a role named "cpox" :)
xdmp:eval('
xquery version "1.0-ml";
import module namespace sec="http://marklogic.com/xdmp/security" at
    "/MarkLogic/security.xqy";
 
if (xdmp:database() eq xdmp:security-database()) then ()
else error(
  (), "NOT-SECURITY",
  text { "this database is not the security database" } )
,
sec:create-role("cpox", "cpox", (), (), ())
', (), <options xmlns="xdmp:eval">
  <database>{ xdmp:security-database() }</database></options>)

