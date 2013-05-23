xquery version "1.0-ml";
(:~ cpox/http/bookmark.xqy
 :
 : Copyright (c) 2009-2010 MarkLogic Corporation. All Rights Reserved.
 :
 : @author Michael Blakeley
 :
 :)

declare variable $USER-ID as xs:string := xdmp:get-request-field('uid');

declare variable $DOC-ID as xs:string := xdmp:get-request-field('did');

let $uri := concat('/users/', $USER-ID, '/bookmarks.xml')
let $parent := doc($uri)/bookmark-list
let $assert := (
  if (empty($parent/bookmark[. eq $DOC-ID])) then ()
  else error((), 'BOOKMARK-EXISTS', text {
      'user', $USER-ID, 'has already bookmarked', $DOC-ID })
)
let $new := element bookmark { $DOC-ID }
let $do := (
  if (exists($parent)) then xdmp:node-insert-child($parent, $new)
  else xdmp:document-insert($uri, element bookmark-list { $new })
)
return 1 + count($parent/bookmark)

(: cpox/http/bookmark.xqy :)