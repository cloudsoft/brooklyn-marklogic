xquery version "1.0-ml";
declare namespace html = "http://www.w3.org/1999/xhtml";
import module namespace sem = "http://marklogic.com/semantics" at "/MarkLogic/semantics.xqy";
sem:nquad-batch-load("/tmp/triples.nt")
