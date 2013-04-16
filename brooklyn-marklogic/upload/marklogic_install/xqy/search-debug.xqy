xquery version "1.0-ml";
(:~ cpox/http/search.xqy
 :
 : Copyright (c) 2009-2010 MarkLogic Corporation. All Rights Reserved.
 :
 : @author Michael Blakeley
 :
 :)

import module namespace search="http://marklogic.com/appservices/search"
  at "/MarkLogic/appservices/search/search.xqy";

declare namespace mw="http://www.mediawiki.org/xml/export-0.4/";

declare variable $MW-NS := "http://www.mediawiki.org/xml/export-0.4/";

declare variable $LANGUAGE as xs:string := xdmp:get-request-field('lang');
declare variable $QUERY as xs:string := xdmp:get-request-field('q');
declare variable $START as xs:integer := xs:integer(
  xdmp:get-request-field('s', '1'));

let $options :=
<options xmlns="http://marklogic.com/appservices/search">
{
  (: language for the query :)
  <term apply="term">
    <empty apply="all-results"/>
    <term-option>lang={ $LANGUAGE }</term-option>
  </term>,

  (: element string facets :)
  let $type := 'xs:string'
  for $fn in ('username', 'ip')
  return element constraint {
    attribute name { $fn },
    element range {
      attribute facet { 'true' },
      attribute type { $type },
      attribute collation {
        "http://marklogic.com/collation/codepoint" },
      element facet-option { 'frequency-order' },
      element facet-option { 'limit=5' },
      element element {
        attribute ns { $MW-NS },
        attribute name { $fn }
      }
    }
  },

  (: mw:timestamp buckets :)
  element constraint {
    attribute name { 'timestamp' },
    element range {
      attribute facet { 'true' },
      attribute type { 'xs:dateTime' },
      element element {
        attribute ns { $MW-NS },
        attribute name { 'timestamp' }
      },
      for $y in 2001 to 2009
      return element bucket {
        attribute ge {
          xs:dateTime(concat(string($y), '-01-01T00:00:00')) },
        attribute lt {
          xs:dateTime(concat(string(1 + $y), '-01-01T00:00:00')) },
        attribute name { $y } }
    }
  },

  (: attribute facet on mw:a/@id :)
  let $type := 'xs:string'
  for $fn in ('id')
  return element constraint {
    attribute name { $fn },
    element range {
      attribute facet { 'true' },
      attribute type { $type },
      attribute collation {
        "http://marklogic.com/collation/codepoint" },
      element facet-option { 'frequency-order' },
      element facet-option { 'limit=5' },
      element element {
        attribute ns { $MW-NS },
        attribute name { 'a' }
      },
      element attribute {
        attribute ns { '' },
        attribute name { $fn }
      }
    }
  },

  (: wiki pages only - otherwise bookmarks show up :)
  element additional-query {
    cts:directory-query('/pages/', 'infinity')
  },

  element return-query { true() },
  element return-metrics { true() }
}
</options>
return (
  xdmp:log(xdmp:quote($options)) (: DEBUG :)
  , search:check-options($options) (: DEBUG :)
  ,
  search:search($QUERY, $options, $START)
)

(: cpox/http/search.xqy :)
