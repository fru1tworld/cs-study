```SQL
mysql> EXPLAIN
        SELECT a
        FROM b
        WHERE a=a
        GROUP BY a;
```

```
+-----+--------+----------+------------+---------------------------------------------+
|   id| table  | type     | key        | Extra                                       |
+-----+--------+----------+------------+---------------------------------------------+
|   1| salaries| range    | PRIMARY    | Using where; Using index for group-by       |
+-----+--------+----------+------------+---------------------------------------------+
```
