xquery version "1.0-ml";


(: create database :)
 
import module namespace admin = "http://marklogic.com/xdmp/admin"
 at "/MarkLogic/admin.xqy" ;
 
declare variable $C as element() := admin:get-configuration();
declare variable $ID := ();
current-dateTime()
, xdmp:set($C, admin:database-create(
  $C, 'MarkMail', xdmp:security-database(), xdmp:schema-database()))
, xdmp:set($ID, admin:database-get-id($C, 'MarkMail'))
, xdmp:set($C, admin:database-set-stemmed-searches(
  $C, $ID, 'advanced'))
, xdmp:set($C, admin:database-set-uri-lexicon($C, $ID, true()))
, xdmp:set($C, admin:database-set-word-searches($C, $ID, true()))
, xdmp:set($C, admin:database-set-element-word-positions($C, $ID, true()))
, xdmp:set($C, admin:database-set-element-value-positions($C, $ID, true()))
, xdmp:set($C, admin:database-set-trailing-wildcard-searches($C, $ID, true()))
, xdmp:set($C, admin:database-set-trailing-wildcard-word-positions($C, $ID, true()))
, xdmp:set($C, admin:database-set-fast-element-trailing-wildcard-searches($C, $ID, true()))
, xdmp:set($C, admin:database-set-fast-case-sensitive-searches($C, $ID, false()))
, xdmp:set($C, admin:database-set-directory-creation($C, $ID, 'manual'))
, xdmp:set($C, admin:database-set-maintain-last-modified($C, $ID, false()))
, xdmp:set($C, admin:database-set-inherit-collections($C, $ID, false()))
, xdmp:set($C, admin:database-set-inherit-permissions($C, $ID, false()))
, xdmp:set($C, admin:database-set-reindexer-enable($C, $ID, false()))
, xdmp:set($C, admin:database-set-rebalancer-enable($C, $ID, false()))
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
, xdmp:set($C, admin:database-add-range-element-index(
  $C, $ID, admin:database-range-element-index(
    'dateTime',
    'http://www.mediawiki.org/xml/export-0.4/', 'timestamp',
    '', false() )))
, xdmp:set($C, admin:database-add-range-element-attribute-index(
  $C, $ID, admin:database-range-element-attribute-index(
    'dateTime',
    '',
    'message',
    '',
    'date',
    '',
    fn:false())))
, xdmp:set($C, admin:database-add-range-element-attribute-index(
  $C, $ID, admin:database-range-element-attribute-index(
    'string',
    '',
    concat('attachment inner-attachment'),
    '',
    'extension',
    'http://marklogic.com/collation/codepoint',
    fn:false())))
, xdmp:set($C, admin:database-add-range-element-attribute-index(
  $C, $ID, admin:database-range-element-attribute-index(
    'string',
    '',
    'message',
    '',
    concat('list type year-month year-month-day'),
    'http://marklogic.com/collation/codepoint',
    fn:false() )))
, xdmp:set($C, admin:database-add-range-element-attribute-index(
  $C, $ID, admin:database-range-element-attribute-index(
    'string',
    '',
    'from',
    '',
    'personal',
    'http://marklogic.com/collation/codepoint',
    fn:false() )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://www.w3.org/1999/xhtml',
    concat(
      'a abbr acronym b big br center cite code dfn em font i kbd',
      ' q samp small span strong sub sup tt var') )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://schemas.microsoft.com/office/word/2003/wordml',
    concat(
      'br cr fldChar fldData fldSimple hlink noBreakHyphen permEnd',
      ' permStart pgNum proofErr r softHyphen sym t tab') )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://schemas.microsoft.com/office/word/2003/auxHint',
    't' )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://marklogic.com/entity',
    concat(
       'person organization location gpe facility religion nationality',
       ' credit-card-number email coordinate money percent id phone-number url utm date time') )))
, xdmp:set($C, admin:database-add-phrase-through(
  $C, $ID, admin:database-phrase-through(
    'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
    concat(
       'r t endnoteReference footnoteReference customXml hyperlink sdt sdtContent commentRangeEnd commentRangeStart',
       ' bookmarkStart bookmarkEnd fldSimple instrText smartTag ins proofErr') )))
, xdmp:set($C, admin:database-add-phrase-around(
  $C, $ID, admin:database-phrase-around(
    'http://schemas.microsoft.com/office/word/2003/wordml',
       'delInstrText delText endnote footnote instrText pict rPr' )))
, xdmp:set($C, admin:database-add-phrase-around(
  $C, $ID, admin:database-phrase-around(
    'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
       'pPr rPr customXmlPr sdtPr commentReference del' )))
, xdmp:set($C, admin:database-add-element-word-query-through(
  $C, $ID, admin:database-element-word-query-through(
    '',
       'email url')))
, xdmp:set($C, admin:database-add-element-word-query-through(
  $C, $ID, admin:database-element-word-query-through(
    'http://schemas.microsoft.com/office/word/2003/wordml',
       'p')))
, xdmp:set($C, admin:database-add-element-word-query-through(
  $C, $ID, admin:database-element-word-query-through(
    'http://schemas.openxmlformats.org/wordprocessingml/2006/main',
       'p')))
, xdmp:set($C, admin:database-add-geospatial-element-pair-index(
  $C, $ID,admin:database-geospatial-element-pair-index(
       '',
       'geo-info',
       '',
       'latitude',
       '',
       'longitude',
       'wgs84',
       fn:false() )))
, $ID
, $C/*/*[last()]
, admin:save-configuration($C)
