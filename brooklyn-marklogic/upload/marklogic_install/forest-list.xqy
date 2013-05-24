xquery version "1.0-ml";

for $FOREST_ID in xdmp:forests()
    return xdmp:forest-name($FOREST_ID)