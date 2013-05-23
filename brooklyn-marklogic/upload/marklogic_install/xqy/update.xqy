xquery version "1.0-ml";
(:~ cpox/http/update.xqy
 :
 : Copyright (c) 2009-2010 MarkLogic Corporation. All Rights Reserved.
 :
 : @author Michael Blakeley
 :
 :)

declare namespace mw="http://www.mediawiki.org/xml/export-0.4/";

declare variable $ID as xs:string := xdmp:get-request-field('id');
declare variable $TOKEN as xs:string := xdmp:get-request-field('token');

let $parent := doc($ID)/mw:page/mw:revision/mw:text
let $assert := (
  if (not(cts:contains($parent, $TOKEN))) then ()
  else error(
    (), 'TOKEN-EXISTS',
    text { 'Token', $TOKEN, 'already exists in', xdmp:describe($parent) } ) )
(: whitespace, so when different tokens are added they stay distinct :)
return xdmp:node-insert-child($parent, text { '', $TOKEN })

(: cpox/http/update.xqy :)
