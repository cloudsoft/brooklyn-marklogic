xquery version "1.0-ml";
(:~ cpox/http/verify.xqy
 :
 : Copyright (c) 2009-2010 MarkLogic Corporation. All Rights Reserved.
 :
 : @author Michael Blakeley
 :
 :)

declare namespace mw="http://www.mediawiki.org/xml/export-0.4/";

declare variable $TOKEN as xs:string := xdmp:get-request-field('token');

xdmp:estimate(
  cts:search(
    doc(),
    cts:element-word-query(
      xs:QName('mw:text'), $TOKEN)))

(: cpox/http/verify.xqy :)
