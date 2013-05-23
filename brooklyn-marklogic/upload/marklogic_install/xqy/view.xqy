xquery version "1.0-ml";
(:~ cpox/http/view.xqy
 :
 : Copyright (c) 2009-2010 MarkLogic Corporation. All Rights Reserved.
 :
 : @author Michael Blakeley
 :
 :)

declare variable $ID as xs:string := xdmp:get-request-field('id');

doc($ID) 

(: cpox/http/view.xqy :)
