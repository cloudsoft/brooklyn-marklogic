xquery version "1.0-ml";

for $DATABASE_ID in xdmp:databases()
    return xdmp:database-name($DATABASE_ID)