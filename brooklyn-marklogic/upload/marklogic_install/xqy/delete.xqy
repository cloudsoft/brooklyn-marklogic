xquery version "1.0-ml";
(:~ cpox/http/delete.xqy
 :
 : Copyright (c) 2009-2012 MarkLogic Corporation. All Rights Reserved.
 :
 :)

declare variable $ID as xs:string := xdmp:get-request-field('id');

if (fn:doc-available($ID)) then xdmp:document-delete($ID) else ()

(: cpox/http/delete.xqy :)
