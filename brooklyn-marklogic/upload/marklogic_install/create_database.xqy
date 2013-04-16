xquery version "1.0-ml";


(: create database :)
 
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy" ;
 
declare variable $C as element() := admin:get-configuration();
declare variable $ID := ();
current-dateTime()
, xdmp:set($C, admin:database-create(
  $C, 'cpox', xdmp:security-database(), xdmp:schema-database()))
, xdmp:set($ID, admin:database-get-id($C, 'cpox'))
, xdmp:set($C, admin:database-set-stemmed-searches(
  $C, $ID, 'decompounding'))
, xdmp:set($C, admin:database-set-uri-lexicon($C, $ID, true()))
, xdmp:set($C, admin:database-set-directory-creation($C, $ID, 'manual'))
, xdmp:set($C, admin:database-set-maintain-last-modified($C, $ID, false()))
, xdmp:set($C, admin:database-set-inherit-collections($C, $ID, false()))
, xdmp:set($C, admin:database-set-inherit-permissions($C, $ID, false()))
, xdmp:set($C, admin:database-set-inherit-quality($C, $ID, false()))
, xdmp:set($C, admin:database-set-in-memory-list-size($C, $ID, 600))
, xdmp:set($C, admin:database-set-in-memory-tree-size($C, $ID, 45))
, xdmp:set($C, admin:database-set-in-memory-range-index-size($C, $ID, 8))
, xdmp:set($C, admin:database-set-in-memory-reverse-index-size($C, $ID, 1))
, xdmp:set($C, admin:database-set-journaling($C, $ID, 'strict'))
, xdmp:set($C, admin:database-set-index-detection($C, $ID, 'none'))
, xdmp:set($C, admin:database-set-expunge-locks($C, $ID, 'none'))
,
(: strip out defaults :)
for $i in admin:database-get-range-element-indexes($C, $ID)
return xdmp:set(
  $C, admin:database-delete-range-element-index($C, $ID, $i) )
,
for $i in admin:database-get-range-element-attribute-indexes($C, $ID)
return xdmp:set(
  $C, admin:database-delete-range-element-attribute-index($C, $ID, $i) )
,
for $i in admin:database-get-phrase-arounds($C, $ID)
return xdmp:set(
  $C, admin:database-delete-phrase-around($C, $ID, $i) )
,
for $i in admin:database-get-phrase-throughs($C, $ID)
return xdmp:set(
  $C, admin:database-delete-phrase-through($C, $ID, $i) )
,
for $i in admin:database-get-element-word-query-throughs($C, $ID)
return xdmp:set(
  $C, admin:database-delete-element-word-query-through($C, $ID, $i) )
(: facets :)
,
xdmp:set($C, admin:database-add-range-element-index(
  $C, $ID, admin:database-range-element-index(
    'dateTime',
    'http://www.mediawiki.org/xml/export-0.4/', 'timestamp',
    '', false() )))
,
xdmp:set($C, admin:database-add-range-element-index(
  $C, $ID, admin:database-range-element-index(
    'string',
    'http://www.mediawiki.org/xml/export-0.4/',
    'ip username',
    'http://marklogic.com/collation/codepoint',
    false() )))
, xdmp:set($C, admin:database-add-range-element-attribute-index(
  $C, $ID, admin:database-range-element-attribute-index(
    'string',
    'http://www.mediawiki.org/xml/export-0.4/', 'a',
    '', 'id',
    'http://marklogic.com/collation/codepoint',
    false() )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://www.mediawiki.org/xml/export-0.4/',
    concat(
      'a abbr acronym b big br center cite code dfn em font i kbd',
      ' q samp small span strong sub sup tt var') )))
, $ID
, $C/*/*[last()]
, admin:save-configuration($C)
