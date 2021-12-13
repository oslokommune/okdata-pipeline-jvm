CSV to CSV transformer
======================

Pipeline component for transforming CSV using [CSVLT](https://github.oslo.kommune.no/origo-dataplatform/csvlt).

CSVLT is based on [JSLT](https://github.com/schibsted/jslt), a [language tutorial](https://github.com/schibsted/jslt/blob/master/tutorial.md) is available.

## Input event

The input event contains the input s3 bucket/key, output bucket/key and the CSVLT expression.

Example Lambda event input:

```json
{
  "input": "s3://ok-origo-dataplatform-dev/incoming/green/test/boligpriser.csv",
  "output": "s3://ok-origo-dataplatform-dev/intermediate/green/test/boligpriser.csv",
  "csvlt": "def zeroes(length)\n  if ($length < 1)\n    \"\"\n  else\n    \"0\" + zeroes($length - 1)\n\ndef delbydel_id(delbydel_nummer)\n  let nr = string(round(number($delbydel_nummer)))\n  zeroes(4 - size($nr)) + $nr\n\n{\n  \"delbydel_id\": delbydel_id(.Delbydelnummer),\n  \"navn\": .Delbydelsnavn\n}\n",
  "delimiter": ";"
}
```

The input event object takes the following key/values:

| Key               | Type      | Description                                       | Default value                     |
| ----------------- | --------- | ------------------------------------------------- | --------------------------------- |
| input             | String    | The S3 input path                                 | <no default, must be supplied>    |
| output            | String    | The S3 output path                                | <no default, must be supplied>    |
| csvlt             | String    | The CSVLT expression to apply                     | <no default, must be supplied>    |
| header_row        | Boolean   | Is the first row of the input file a header row?  | `true`                            |
| delimiter         | String    | The CSV delimiter used, ie. ',' or ';'            | `;`                               |
| record_separator  | String    | Record/row separator, ie '\n'                     | `\n`                              |
| quote             | String    | Quote marks used, ie '"'                          | `"`                               |
